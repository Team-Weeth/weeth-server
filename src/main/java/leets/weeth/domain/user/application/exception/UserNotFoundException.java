package leets.weeth.domain.user.application.exception;

import leets.weeth.global.common.exception.BusinessLogicException;

public class UserNotFoundException extends BusinessLogicException {
    public UserNotFoundException() {
        super(UserErrorCode.USER_NOT_FOUND);
    }
}