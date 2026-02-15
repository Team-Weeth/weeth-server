package com.weeth.domain.penalty.application.exception;

import com.weeth.global.common.exception.BaseException;

public class PenaltyNotFoundException extends BaseException {
    public PenaltyNotFoundException() {
        super(PenaltyErrorCode.PENALTY_NOT_FOUND);
    }
}
