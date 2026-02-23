package com.pm.greatadamu.accountservice.exception;

import com.pm.greatadamu.accountservice.model.AccountType;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String accountNumber) {
        super("Account with number " + accountNumber + " not found");
    }

}

