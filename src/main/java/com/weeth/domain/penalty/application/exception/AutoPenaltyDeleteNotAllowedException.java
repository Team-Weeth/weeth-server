package com.weeth.domain.penalty.application.exception;

import com.weeth.global.common.exception.BaseException;

public class AutoPenaltyDeleteNotAllowedException extends BaseException {
    public AutoPenaltyDeleteNotAllowedException() {
        super(PenaltyErrorCode.AUTO_PENALTY_DELETE_NOT_ALLOWED);
    }
}
