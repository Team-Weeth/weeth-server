package com.weeth.domain.club.application.exception

import com.weeth.global.common.exception.BaseException

class ClubJoinLimitExceededException : BaseException(ClubErrorCode.CLUB_JOIN_LIMIT_EXCEEDED)
