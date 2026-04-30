package com.weeth.domain.club.application.exception

import com.weeth.global.common.exception.BaseException

class SelfBanNotAllowedException : BaseException(ClubErrorCode.SELF_BAN_NOT_ALLOWED)
