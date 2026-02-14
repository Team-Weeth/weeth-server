package leets.weeth.domain.user.application.exception;

import leets.weeth.global.common.exception.BusinessLogicException;

public class EmailNotFoundException extends BusinessLogicException {
    public EmailNotFoundException() {
        super(UserErrorCode.EMAIL_NOT_FOUND);
    }
}
