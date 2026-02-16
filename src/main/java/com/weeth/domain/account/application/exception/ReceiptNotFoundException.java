package com.weeth.domain.account.application.exception;

import com.weeth.global.common.exception.BaseException;

public class ReceiptNotFoundException extends BaseException {
    public ReceiptNotFoundException() {
        super(AccountErrorCode.RECEIPT_NOT_FOUND);
    }
}
