package com.weeth.domain.user.application.exception

import com.weeth.global.common.exception.BaseException

class StudentIdExistsException : BaseException(UserErrorCode.STUDENT_ID_EXISTS)
