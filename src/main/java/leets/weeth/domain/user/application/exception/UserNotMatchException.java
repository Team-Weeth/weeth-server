package leets.weeth.domain.user.application.exception;

import leets.weeth.global.common.exception.BusinessLogicException;

public class UserNotMatchException extends BusinessLogicException {
    public UserNotMatchException() {
        super(UserErrorCode.USER_NOT_MATCH);
    }
}
