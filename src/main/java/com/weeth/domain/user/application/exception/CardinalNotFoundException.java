package com.weeth.domain.user.application.exception;

import com.weeth.global.common.exception.BaseException;

public class CardinalNotFoundException extends BaseException {
    public CardinalNotFoundException() {
        super(UserErrorCode.CARDINAL_NOT_FOUND);
    }
}
