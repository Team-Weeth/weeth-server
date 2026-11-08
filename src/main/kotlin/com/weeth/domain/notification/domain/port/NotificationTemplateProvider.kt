package com.weeth.domain.notification.domain.port

import com.weeth.domain.notification.domain.enums.NotificationChannel
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.vo.NotificationTemplate

/** 유형·채널별 템플릿 조회. version=null이면 현재 버전, 지정한 버전이 없으면 null을 반환한다. */
interface NotificationTemplateProvider {
    fun find(
        type: NotificationType,
        channel: NotificationChannel,
        version: Int? = null,
    ): NotificationTemplate?
}
