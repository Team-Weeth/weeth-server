package com.weeth.domain.comment.application.exception

import com.weeth.global.common.exception.BaseException

class CommentAlreadyDeletedException : BaseException(CommentErrorCode.COMMENT_ALREADY_DELETED)
