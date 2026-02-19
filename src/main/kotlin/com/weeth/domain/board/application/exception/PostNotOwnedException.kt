package com.weeth.domain.board.application.exception

import com.weeth.global.common.exception.BaseException

class PostNotOwnedException : BaseException(BoardErrorCode.POST_NOT_OWNED)
