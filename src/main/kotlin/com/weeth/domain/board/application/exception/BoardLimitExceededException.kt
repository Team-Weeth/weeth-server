package com.weeth.domain.board.application.exception

import com.weeth.global.common.exception.BaseException

class BoardLimitExceededException : BaseException(BoardErrorCode.BOARD_LIMIT_EXCEEDED)
