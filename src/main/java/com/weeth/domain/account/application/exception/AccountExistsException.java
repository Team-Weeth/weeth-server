package com.weeth.domain.account.application.exception;

import com.weeth.global.common.exception.BaseException;

public class AccountExistsException extends BaseException {
    public AccountExistsException() {
        super(AccountErrorCode.ACCOUNT_EXISTS);
    }
}

