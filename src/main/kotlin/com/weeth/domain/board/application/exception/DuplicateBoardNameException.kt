package com.weeth.domain.board.application.exception

import com.weeth.global.common.exception.BaseException

class DuplicateBoardNameException : BaseException(BoardErrorCode.DUPLICATE_BOARD_NAME)
