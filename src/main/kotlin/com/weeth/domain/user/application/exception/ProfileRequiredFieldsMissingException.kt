package com.weeth.domain.user.application.exception

import com.weeth.global.common.exception.BaseException

class ProfileRequiredFieldsMissingException : BaseException(UserErrorCode.PROFILE_REQUIRED_FIELDS_MISSING)
