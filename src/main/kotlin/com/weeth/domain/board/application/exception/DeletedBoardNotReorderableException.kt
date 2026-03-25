package com.weeth.domain.board.application.exception

import com.weeth.global.common.exception.BaseException

class DeletedBoardNotReorderableException : BaseException(BoardErrorCode.DELETED_BOARD_NOT_REORDERABLE)
