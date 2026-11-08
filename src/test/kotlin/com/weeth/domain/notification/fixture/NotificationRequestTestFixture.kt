package com.weeth.domain.notification.fixture

import com.weeth.domain.notification.domain.enums.NotificationReferenceType
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.vo.NotificationRequest
import com.weeth.global.common.id.TsidBase62Encoder

object NotificationRequestTestFixture {
    fun createNotice(
        clubId: Long = 62L,
        postId: Long = 100L,
        title: String? = "공지",
    ) = NotificationRequest(
        eventId = "NOTICE_CREATED:$clubId:$postId",
        type = NotificationType.NOTICE_CREATED,
        variables = mapOf("noticeTitle" to title),
        targetPath = "/clubs/${TsidBase62Encoder.encode(clubId)}/boards/10/posts/$postId",
        clubId = clubId,
        referenceType = NotificationReferenceType.POST,
        referenceId = postId,
        data = mapOf("referenceType" to "POST"),
    )
}
