package com.weeth.domain.board.application.exception

import com.weeth.global.common.exception.BaseException

class PostLikeLockTimeoutException : BaseException(BoardErrorCode.POST_LIKE_LOCK_TIMEOUT)
