package com.weeth.domain.penalty.application.exception

import com.weeth.global.common.exception.BaseException

class PenaltyNotFoundException : BaseException(PenaltyErrorCode.PENALTY_NOT_FOUND)
