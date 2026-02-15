package com.weeth.domain.account.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class AccountExistsException extends BusinessLogicException {
    public AccountExistsException() {
        super(AccountErrorCode.ACCOUNT_EXISTS);
    }
}

