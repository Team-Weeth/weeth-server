package leets.weeth.domain.user.application.exception;

import leets.weeth.global.common.exception.BusinessLogicException;

public class UserInActiveException extends BusinessLogicException {
    public UserInActiveException() {
        super(UserErrorCode.USER_INACTIVE);
    }
}
