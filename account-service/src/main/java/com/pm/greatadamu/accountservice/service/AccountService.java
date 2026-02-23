package com.pm.greatadamu.accountservice.service;

import com.pm.greatadamu.accountservice.dto.AccountRequestDto;
import com.pm.greatadamu.accountservice.dto.AccountResponseDto;
import com.pm.greatadamu.accountservice.exception.AccountNotFoundException;
import com.pm.greatadamu.accountservice.exception.AccountTypeAlreadyExistException;
import com.pm.greatadamu.accountservice.mapper.AccountMapper;
import com.pm.greatadamu.accountservice.model.Account;
import com.pm.greatadamu.accountservice.model.AccountType;
import com.pm.greatadamu.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    public AccountResponseDto createAccount(AccountRequestDto accountRequestDto) {
        //check if customer already has this account type
        Optional<Account> existingAccount = accountRepository.findByCustomerIdAndAccountType(
                accountRequestDto.getCustomerId(),
                accountRequestDto.getAccountType()
        );
        if (existingAccount.isPresent()) {
            throw new AccountTypeAlreadyExistException(accountRequestDto.getAccountType());
        }
            //take user response and map to account entity->CREATE ACCOUNT
            Account account = accountMapper.mapToEntity(accountRequestDto);

          if (account.getAccountType() == AccountType.CREDIT) {
              account.setAccountBalance(new BigDecimal("1000.00"));
          }

            // save entity to DB(insert into accounts table)
            Account savedAccount=  accountRepository.save(account);

            //send it(entity) to responseDTO so user can see account created
            return accountMapper.mapToResponse(savedAccount);
        }


    public AccountResponseDto updateAccount(Long id,AccountRequestDto accountRequestDto) {
        //get account from db
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));
        //update only the fields that are allowed to change
           account.setAccountName(accountRequestDto.getAccountName());
           account.setAccountType(accountRequestDto.getAccountType());

           //save updated account
        Account updatedAccount = accountRepository.save(account);

              //show user the update made
        return accountMapper.mapToResponse(updatedAccount);
    }

    public AccountResponseDto getAccountByAccountNumber(String accountNumber){
        //get acct from db and save it in acct entity
        Account acct = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        return accountMapper.mapToResponse(acct);
    }

    public List<AccountResponseDto> getAccountsByCustomerId(Long customerId){
        List<Account> accounts = accountRepository.findByCustomerId(customerId);
        return accounts.stream()
                .map(accountMapper ::mapToResponse)
                .toList();
    }

    public void deleteAccountByAccountNumber(String accountNumber){
        //get account by account from db and delete
        Account account = accountRepository.findByAccountNumber(accountNumber)
                        .orElseThrow(() ->new AccountNotFoundException(accountNumber));
        accountRepository.delete(account);

    }


}
