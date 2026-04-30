package com.weeth.domain.club.application.exception

import com.weeth.global.common.exception.BaseException

class SelfRoleChangeNotAllowedException : BaseException(ClubErrorCode.SELF_ROLE_CHANGE_NOT_ALLOWED)
