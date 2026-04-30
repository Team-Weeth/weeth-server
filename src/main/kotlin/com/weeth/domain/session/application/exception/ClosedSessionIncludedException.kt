package com.weeth.domain.session.application.exception

import com.weeth.domain.session.application.dto.response.ClosedSessionCountResponse
import com.weeth.global.common.exception.BaseException

class ClosedSessionIncludedException(
    errorCode: SessionErrorCode,
    closedSessionCount: Int,
) : BaseException(
        errorCode = errorCode,
        data = ClosedSessionCountResponse(closedSessionCount),
    )
