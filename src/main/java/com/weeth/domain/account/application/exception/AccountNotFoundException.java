package com.weeth.domain.account.application.exception;

import com.weeth.global.common.exception.BaseException;

public class AccountNotFoundException extends BaseException {
    public AccountNotFoundException() {
        super(AccountErrorCode.ACCOUNT_NOT_FOUND);
    }
}
