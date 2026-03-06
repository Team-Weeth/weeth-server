package com.weeth.domain.club.application.exception

import com.weeth.global.common.exception.BaseException

class ClubNotFoundException : BaseException(ClubErrorCode.CLUB_NOT_FOUND)
