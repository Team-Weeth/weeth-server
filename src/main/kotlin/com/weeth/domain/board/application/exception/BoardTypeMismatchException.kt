package com.weeth.domain.board.application.exception

import com.weeth.global.common.exception.BaseException

class BoardTypeMismatchException : BaseException(BoardErrorCode.BOARD_TYPE_MISMATCH)
