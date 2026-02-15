package com.weeth.domain.account.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class ReceiptNotFoundException extends BusinessLogicException {
    public ReceiptNotFoundException() {
        super(AccountErrorCode.RECEIPT_NOT_FOUND);
    }
}
