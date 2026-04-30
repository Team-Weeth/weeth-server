package com.weeth.domain.board.application.exception

import com.weeth.global.common.exception.BaseException

class PageNotFoundException : BaseException(BoardErrorCode.PAGE_NOT_FOUND)
