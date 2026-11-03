package com.weeth.domain.feedback.application.exception

import com.weeth.global.common.exception.BaseException

class InvalidFeedbackContentException : BaseException(FeedbackErrorCode.INVALID_FEEDBACK_CONTENT)
