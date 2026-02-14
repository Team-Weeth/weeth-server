package leets.weeth.domain.user.application.exception;

import leets.weeth.global.common.exception.BusinessLogicException;

public class DuplicateCardinalException extends BusinessLogicException {
    public DuplicateCardinalException() {
        super(UserErrorCode.DUPLICATE_CARDINAL);
    }
}
