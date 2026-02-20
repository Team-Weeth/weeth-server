package com.weeth.domain.user.application.exception

import com.weeth.global.common.exception.BaseException

class CardinalNotFoundException : BaseException(UserErrorCode.CARDINAL_NOT_FOUND)
