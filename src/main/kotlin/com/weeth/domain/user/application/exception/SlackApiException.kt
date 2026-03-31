package com.weeth.domain.user.application.exception

import com.weeth.global.common.exception.BaseException

class SlackApiException(
    cause: Throwable? = null,
) : BaseException(UserErrorCode.SLACK_API_ERROR) {
    init {
        initCause(cause)
    }
}
