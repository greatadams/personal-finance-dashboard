package com.pm.greatadamu.transactionservice.gRPC;

import com.pm.greatadamu.grpc.account.*;
import com.pm.greatadamu.grpc.account.AccountServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class AccountGrpcClient {

    @Value("${grpc.account-service.host:localhost}")
    private String accountServiceHost;

    @Value("${grpc.account-service.port:9090}")
    private int accountServicePort;

    @Value("${grpc.deadline-ms:2000}")
    private long deadlineMs;

    private ManagedChannel channel;
    private AccountServiceGrpc.AccountServiceBlockingStub blockingStub;

    @PostConstruct
    public void init() {
        log.info("Initializing gRPC client for Account Service at {}:{}",
                accountServiceHost, accountServicePort);

        channel = ManagedChannelBuilder
                .forAddress(accountServiceHost, accountServicePort)
                .usePlaintext()
                .build();

        blockingStub = AccountServiceGrpc.newBlockingStub(channel);

        log.info("gRPC client initialized successfully");
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            log.info("Shutting down gRPC channel");
            channel.shutdown();
        }
    }

    /**
     * Validate if account exists and is active
     */
    public ValidateAccountResponse validateAccount(Long accountId) {
        try {
            log.info("gRPC Client: Validating account ID: {}", accountId);

            ValidateAccountRequest request = ValidateAccountRequest.newBuilder()
                    .setAccountId(accountId)
                    .build();

            //deadline
            return blockingStub
                    .withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
                    .validateAccount(request);
        }catch (StatusRuntimeException e) {
            // handle gRPC status errors
            log.error("validateAccount failed: status={}, msg={}", e.getStatus(), e.getMessage());
            throw e; // or translate into your service exception
        }

    }
    /**
     * Get account balance
     */
    public GetBalanceResponse getBalance(Long accountId) {
        try {
            GetBalanceRequest request = GetBalanceRequest.newBuilder()
                    .setAccountId(accountId)
                    .build();

            // apply deadline
            return blockingStub
                    .withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
                    .getBalance(request);

        } catch (StatusRuntimeException e) {
            log.error("getBalance failed: status={}, msg={}", e.getStatus(), e.getMessage());
            throw e;
        }
    }

    /**
     * Debit account (subtract from balance)
     */
    public UpdateBalanceResponse debitAccount(Long accountId, BigDecimal amount, String description) {
        try {
            UpdateBalanceRequest request = UpdateBalanceRequest.newBuilder()
                    .setAccountId(accountId)
                    .setAmount(amount.toString())
                    .setOperation(OperationType.DEBIT)
                    .setDescription(description == null ? "" : description)
                    .build();

            //  apply deadline
            return blockingStub
                    .withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
                    .updateBalance(request);

        } catch (StatusRuntimeException e) {
            log.error("debitAccount failed: status={}, msg={}", e.getStatus(), e.getMessage());
            throw e;
        }
    }

    /**
     * Credit account (add to balance)
     */
    public UpdateBalanceResponse creditAccount(Long accountId, BigDecimal amount, String description) {
        try {
            UpdateBalanceRequest request = UpdateBalanceRequest.newBuilder()
                    .setAccountId(accountId)
                    .setAmount(amount.toString())
                    .setOperation(OperationType.CREDIT)
                    .setDescription(description == null ? "" : description)
                    .build();

            // apply deadline
            return blockingStub
                    .withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
                    .updateBalance(request);

        } catch (StatusRuntimeException e) {
            log.error("creditAccount failed: status={}, msg={}", e.getStatus(), e.getMessage());
            throw e;
        }
    }

}
