package com.weeth.domain.penalty.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class PenaltyNotFoundException extends BusinessLogicException {
    public PenaltyNotFoundException() {
        super(PenaltyErrorCode.PENALTY_NOT_FOUND);
    }
}
