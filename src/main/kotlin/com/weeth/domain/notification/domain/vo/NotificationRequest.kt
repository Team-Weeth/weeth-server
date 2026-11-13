package com.weeth.domain.notification.domain.vo

import com.weeth.domain.notification.domain.enums.NotificationReferenceType
import com.weeth.domain.notification.domain.enums.NotificationType

/** 문구 대신 렌더링 변수와 이동 메타데이터를 전달하는 알림 생성 명세. */
data class NotificationRequest(
    val eventId: String,
    val type: NotificationType,
    val variables: Map<String, String?>,
    val targetPath: String,
    val clubId: Long,
    val referenceType: NotificationReferenceType,
    val referenceId: Long,
    val data: Map<String, String>,
)
