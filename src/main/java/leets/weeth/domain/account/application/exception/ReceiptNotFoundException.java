package leets.weeth.domain.account.application.exception;

import leets.weeth.global.common.exception.BusinessLogicException;

public class ReceiptNotFoundException extends BusinessLogicException {
    public ReceiptNotFoundException() {
        super(AccountErrorCode.RECEIPT_NOT_FOUND);
    }
}
