package leets.weeth.domain.account.application.exception;

import leets.weeth.global.common.exception.BusinessLogicException;

public class AccountNotFoundException extends BusinessLogicException {
    public AccountNotFoundException() {
        super(AccountErrorCode.ACCOUNT_NOT_FOUND);
    }
}
