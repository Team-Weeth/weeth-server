package com.weeth.domain.board.application.exception

import com.weeth.global.common.exception.BaseException

class DuplicateBoardIdException : BaseException(BoardErrorCode.DUPLICATE_BOARD_ID)
