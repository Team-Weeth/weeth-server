package com.weeth.domain.cardinal.application.exception

import com.weeth.global.common.exception.BaseException

class CardinalNotFoundException : BaseException(CardinalErrorCode.CARDINAL_NOT_FOUND)
