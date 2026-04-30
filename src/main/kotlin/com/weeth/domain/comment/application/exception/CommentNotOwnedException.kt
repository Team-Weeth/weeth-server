package com.weeth.domain.comment.application.exception

import com.weeth.global.common.exception.BaseException

class CommentNotOwnedException : BaseException(CommentErrorCode.COMMENT_NOT_OWNED)
