package com.weeth.domain.notification.application.exception

import com.weeth.global.common.exception.BaseException

class InvalidNotificationPreferenceException(
    errorCode: NotificationErrorCode,
) : BaseException(errorCode)
