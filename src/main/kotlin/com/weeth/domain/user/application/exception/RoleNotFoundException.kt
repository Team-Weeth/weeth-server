package com.weeth.domain.user.application.exception

import com.weeth.global.common.exception.BaseException

class RoleNotFoundException : BaseException(UserErrorCode.ROLE_NOT_FOUND)
