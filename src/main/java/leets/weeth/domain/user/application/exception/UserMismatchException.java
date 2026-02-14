package leets.weeth.domain.user.application.exception;

import leets.weeth.global.common.exception.BusinessLogicException;

public class UserMismatchException extends BusinessLogicException {
    public UserMismatchException() {
        super(UserErrorCode.USER_MISMATCH);
    }
}