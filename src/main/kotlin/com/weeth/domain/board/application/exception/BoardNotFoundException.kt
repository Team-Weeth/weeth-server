package com.weeth.domain.board.application.exception

import com.weeth.global.common.exception.BaseException

class BoardNotFoundException : BaseException(BoardErrorCode.BOARD_NOT_FOUND)
