package com.weeth.domain.account.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class AccountNotFoundException extends BusinessLogicException {
    public AccountNotFoundException() {
        super(AccountErrorCode.ACCOUNT_NOT_FOUND);
    }
}
