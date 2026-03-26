package com.weeth.domain.board.application.exception

import com.weeth.global.common.exception.BaseException

class BoardCreateLockTimeoutException : BaseException(BoardErrorCode.BOARD_CREATE_LOCK_TIMEOUT)
