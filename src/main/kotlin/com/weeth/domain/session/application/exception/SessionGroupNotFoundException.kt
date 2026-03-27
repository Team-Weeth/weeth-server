package com.weeth.domain.session.application.exception

import com.weeth.global.common.exception.BaseException

class SessionGroupNotFoundException : BaseException(SessionErrorCode.SESSION_GROUP_NOT_FOUND)
