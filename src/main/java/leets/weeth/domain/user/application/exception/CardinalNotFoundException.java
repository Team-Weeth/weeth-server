package leets.weeth.domain.user.application.exception;

import leets.weeth.global.common.exception.BusinessLogicException;

public class CardinalNotFoundException extends BusinessLogicException {
    public CardinalNotFoundException() {
        super(UserErrorCode.CARDINAL_NOT_FOUND);
    }
}
