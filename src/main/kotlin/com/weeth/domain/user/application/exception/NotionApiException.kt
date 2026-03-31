package com.weeth.domain.user.application.exception

import com.weeth.global.common.exception.BaseException

class NotionApiException(
    cause: Throwable? = null,
) : BaseException(UserErrorCode.NOTION_API_ERROR) {
    init {
        initCause(cause)
    }
}
