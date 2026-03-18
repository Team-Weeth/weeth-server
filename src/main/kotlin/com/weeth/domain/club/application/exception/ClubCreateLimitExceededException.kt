package com.weeth.domain.club.application.exception

import com.weeth.global.common.exception.BaseException

class ClubCreateLimitExceededException : BaseException(ClubErrorCode.CLUB_CREATE_LIMIT_EXCEEDED)
