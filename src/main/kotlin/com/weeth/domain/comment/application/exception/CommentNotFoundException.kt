package com.weeth.domain.comment.application.exception

import com.weeth.global.common.exception.BaseException

class CommentNotFoundException : BaseException(CommentErrorCode.COMMENT_NOT_FOUND)
