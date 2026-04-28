package com.weeth.domain.comment.application.exception

import com.weeth.global.common.exception.BaseException

class CommentNotAllowedException : BaseException(CommentErrorCode.COMMENT_NOT_ALLOWED)
