package com.weeth.domain.notification.application.dto.response

import com.weeth.domain.notification.domain.enums.NotificationType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class NotificationResponse(
    @field:Schema(description = "알림 ID", example = "1")
    val id: Long,
    @field:Schema(description = "알림 유형", example = "NOTICE_CREATED")
    val type: NotificationType,
    @field:Schema(description = "알림 제목", example = "새 공지가 등록되었습니다")
    val title: String,
    @field:Schema(description = "알림 내용", example = "중간고사 기간 공지")
    val body: String,
    @field:Schema(description = "알림 클릭 시 이동할 웹 내부 경로", example = "/clubs/1/boards/10/posts/100")
    val targetPath: String,
    @field:Schema(description = "동아리 ID", example = "1")
    val clubId: Long,
    @field:Schema(description = "게시판 ID", example = "10")
    val boardId: Long,
    @field:Schema(description = "게시글 ID", example = "100")
    val postId: Long,
    @field:Schema(description = "읽음 여부", example = "false")
    val isRead: Boolean,
    @field:Schema(description = "알림 생성 시각", example = "2026-09-21T10:00:00")
    val createdAt: LocalDateTime,
    @field:Schema(description = "읽은 시각", nullable = true, example = "2026-09-21T10:05:00")
    val readAt: LocalDateTime? = null,
)
