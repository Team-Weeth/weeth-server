package com.weeth.domain.club.application.exception

import com.weeth.global.common.exception.BaseException

class AlreadyJoinedException : BaseException(ClubErrorCode.ALREADY_JOINED)
