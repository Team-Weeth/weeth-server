package com.weeth.domain.penalty.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class AutoPenaltyDeleteNotAllowedException extends BusinessLogicException {
    public AutoPenaltyDeleteNotAllowedException() {
        super(PenaltyErrorCode.AUTO_PENALTY_DELETE_NOT_ALLOWED);
    }
}
