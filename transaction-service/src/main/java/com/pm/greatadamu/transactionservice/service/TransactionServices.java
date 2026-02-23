package com.pm.greatadamu.transactionservice.service;

import com.pm.greatadamu.grpc.account.UpdateBalanceResponse;
import com.pm.greatadamu.grpc.account.ValidateAccountResponse;
import com.pm.greatadamu.transactionservice.dto.TransactionRequestDTO;
import com.pm.greatadamu.transactionservice.dto.TransactionResponseDTO;
import com.pm.greatadamu.transactionservice.exception.IdempotencyConflictReturnExisting;
import com.pm.greatadamu.transactionservice.gRPC.AccountGrpcClient;
import com.pm.greatadamu.transactionservice.kafka.TransactionEvent;
import com.pm.greatadamu.transactionservice.kafka.TransactionEventProducer;
import com.pm.greatadamu.transactionservice.mapper.TransactionMapper;
import com.pm.greatadamu.transactionservice.model.Transaction;
import com.pm.greatadamu.transactionservice.model.TransactionStatus;
import com.pm.greatadamu.transactionservice.repository.TransactionRepository;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServices {
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final TransactionEventProducer transactionEventProducer;
    private final AccountGrpcClient accountGrpcClient;
    private final TransactionPersistenceService transactionPersistenceService;

    //Return Transaction record from dB
    public List<TransactionResponseDTO> getTransactions() {
        //get all transaction from DB
        List<Transaction> transactions = transactionRepository.findAll();

        //map transaction to transactionResponseDTO using transactionMapper
        //CONVERT EACH TRANSACTION->TransactionResponseDTO
        return transactions.stream()
                .map(transactionMapper::mapToResponseDTO)
                .toList();
    }



    @Transactional
    public TransactionResponseDTO createTransaction(TransactionRequestDTO dto) {
        log.info("Creating transaction: {} from account {} to account {}",
                dto.getAmount(),
                dto.getFromAccountId(),
                dto.getToAccountId());
        //validate amount early
        if (dto.getAmount() == null || dto.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        // CHANGE: Idempotency (prevents duplicate transfer if request retries)
        // Assumption: dto has an idempotencyKey OR you generate one and pass from controller.
        // If you don't have it in dto yet, add it.
        String idemKey = dto.getIdempotencyKey();
        if (idemKey == null || idemKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency key is required");
        }

        //  if this key already exists, return existing transaction (NO double debit)
        transactionRepository.findByIdempotencyKey(idemKey)
                .map(transactionMapper::mapToResponseDTO)
                .ifPresent(existing -> { throw new IdempotencyConflictReturnExisting (existing); });

        // ========== STEP 1: Validate Source Account ==========
        ValidateAccountResponse sourceValidation;
       try{
         sourceValidation=  accountGrpcClient.validateAccount(dto.getFromAccountId());
       }catch (StatusRuntimeException e){
           throw new RuntimeException("Account validation failed (source): " + e.getStatus(), e);
       }


        if (!sourceValidation.getExists()) {
            throw new RuntimeException("Source account not found");
        }
        if (!sourceValidation.getIsActive()) {
            throw new RuntimeException("Source account is not active");
        }

        // ========== STEP 2: Validate Destination Account ==========
        ValidateAccountResponse destValidation;
        try{
           destValidation= accountGrpcClient.validateAccount(dto.getToAccountId());
        }catch (StatusRuntimeException e){
            throw new RuntimeException("Account validation failed (dest): " + e.getStatus(), e);
        }


        if (!destValidation.getExists()) {
            throw new RuntimeException("Destination account not found");
        }
        if (!destValidation.getIsActive()) {
            throw new RuntimeException("Destination account is not active");
        }

        // ========== STEP 3: Create Transaction with PENDING Status ==========
        Transaction transaction = transactionMapper.mapToEntity(dto);
        transaction.setTransactionStatus(TransactionStatus.PENDING);
        transaction.setTransactionDate(LocalDateTime.now());

        //store idempotency key on the entity (must exist in your Transaction model)
        transaction.setIdempotencyKey(idemKey);

        // Save transaction as PENDING
        Transaction savedTransaction = transactionPersistenceService.savePending(transaction);
        log.info("Transaction created with ID: {} and status: PENDING", savedTransaction.getId());


            // ========== STEP 4: Debit Source Account via gRPC ==========
            String description = "Transaction #" + savedTransaction.getId();
        UpdateBalanceResponse debitResponse;
        try {
             debitResponse = accountGrpcClient.debitAccount(
                    dto.getFromAccountId(),
                    dto.getAmount(),
                    description
            );

        }catch (StatusRuntimeException e){
            // map key gRPC statuses to meaningful messages
            if (e.getStatus().getCode() == Status.Code.FAILED_PRECONDITION) {
                throw new RuntimeException("Insufficient funds", e);
            }
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw new RuntimeException("Source account not found", e);
            }
            if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                throw new RuntimeException("Debit timed out; try again", e);
            }
            throw new RuntimeException("Debit failed: " + e.getStatus(), e);
        }
        // If you updated account-service to use gRPC Status errors,
        // debitResponse.getSuccess() will always be true on success, otherwise exception thrown.
        log.info("Source account {} debited. New balance: {}",
                dto.getFromAccountId(), debitResponse.getNewBalance());

            // ========== STEP 5: Credit Destination Account via gRPC ==========
        UpdateBalanceResponse creditResponse;
        try{
            creditResponse = accountGrpcClient.creditAccount(
                    dto.getToAccountId(),
                    dto.getAmount(),
                    description
            );

            if (!creditResponse.getSuccess()) {
                // ROLLBACK: Credit back to source account
                log.error("Failed to credit destination account, rolling back...");
                accountGrpcClient.creditAccount(
                        dto.getFromAccountId(),
                        dto.getAmount(),
                        "Rollback: " + description
                );
                throw new RuntimeException("Failed to credit destination account: " + creditResponse.getMessage());
            }

            log.info("Destination account {} credited. New balance: {}",
                    dto.getToAccountId(), creditResponse.getNewBalance());

            // ========== STEP 6: Update Transaction to COMPLETED ==========
            savedTransaction.setTransactionStatus(TransactionStatus.COMPLETED);
            savedTransaction.setTransactionDate(LocalDateTime.now());
            savedTransaction=transactionPersistenceService.markCompleted(savedTransaction);

            log.info("Transaction {} completed successfully", savedTransaction.getId());

            // ========== STEP 7: Publish to Kafka ==========
            TransactionEvent event = new TransactionEvent(
                    savedTransaction.getId(),
                    savedTransaction.getCustomerId(),
                    savedTransaction.getAmount(),
                    savedTransaction.getTransactionType(),
                    savedTransaction.getTransactionStatus(),
                    savedTransaction.getTransactionDate()
            );
            transactionEventProducer.sendTransactionEvent(event);
            log.info("TransactionEvent published for transaction ID: {}", savedTransaction.getId());

        } catch (Exception e) {
            // ========== ERROR: Update Transaction to FAILED ==========
            log.error("Transaction {} failed: {}", savedTransaction.getId(), e.getMessage());
            savedTransaction.setTransactionStatus(TransactionStatus.FAILED);
            savedTransaction.setTransactionDate(LocalDateTime.now());
            transactionPersistenceService.markFailed(savedTransaction,e.getMessage());

            throw new RuntimeException("Transaction failed: " + e.getMessage());
        }

        // ========== STEP 8: Return Response ==========
        return transactionMapper.mapToResponseDTO(savedTransaction);
    }

    //Get a particular Transaction By Transaction id
    public TransactionResponseDTO getTransactionByTransactionId(Long transactionId) {
        //get transaction ID from dB
        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(()->
                new RuntimeException("Transaction with id " + transactionId + " not found"));

        //convert to JSON->Transaction entity to TransactionResponseDto
        return transactionMapper.mapToResponseDTO(transaction);
    }

    //Get all Transaction done By customer via id
    public List<TransactionResponseDTO> getTransactionsByCustomerId(Long customerId) {
        //get customer ID from dB
        List<Transaction> transactions = transactionRepository.findByCustomerId(customerId);

        return transactions.stream()
                .map(transactionMapper::mapToResponseDTO)
                .toList();
    }

}
