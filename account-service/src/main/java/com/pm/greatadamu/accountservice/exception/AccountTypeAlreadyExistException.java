package com.pm.greatadamu.accountservice.exception;

import com.pm.greatadamu.accountservice.model.AccountType;

public class AccountTypeAlreadyExistException extends RuntimeException {
    public AccountTypeAlreadyExistException(AccountType accountType) {
        super("you already have a " + accountType + " account type");
    }
}
