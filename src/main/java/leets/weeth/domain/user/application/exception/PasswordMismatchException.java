package leets.weeth.domain.user.application.exception;

import leets.weeth.global.common.exception.BusinessLogicException;

public class PasswordMismatchException extends BusinessLogicException {
    public PasswordMismatchException() {
        super(UserErrorCode.PASSWORD_MISMATCH);
    }
}
