package com.weeth.domain.notification.application.exception

import com.weeth.global.common.exception.BaseException

class NotificationNotFoundException : BaseException(NotificationErrorCode.NOTIFICATION_NOT_FOUND)
