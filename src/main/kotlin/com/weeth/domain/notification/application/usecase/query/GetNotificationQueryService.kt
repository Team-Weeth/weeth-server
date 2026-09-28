package com.weeth.domain.notification.application.usecase.query

import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.notification.application.dto.response.NotificationResponse
import com.weeth.domain.notification.application.dto.response.UnreadNotificationCountResponse
import com.weeth.domain.notification.application.mapper.NotificationMapper
import com.weeth.domain.notification.domain.repository.UserNotificationReader
import com.weeth.global.common.response.PageResponse
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetNotificationQueryService(
    private val clubMemberPolicy: ClubMemberPolicy,
    private val userNotificationReader: UserNotificationReader,
    private val notificationMapper: NotificationMapper,
) {
    fun countUnread(
        clubId: Long,
        userId: Long,
    ): UnreadNotificationCountResponse {
        clubMemberPolicy.getActiveMember(clubId, userId)
        return UnreadNotificationCountResponse(
            count = userNotificationReader.countByUserIdAndClubIdAndIsReadFalse(userId, clubId),
        )
    }

    fun findAll(
        clubId: Long,
        userId: Long,
        page: Int,
        size: Int,
    ): PageResponse<NotificationResponse> {
        clubMemberPolicy.getActiveMember(clubId, userId)
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, MAX_PAGE_SIZE))
        return PageResponse.from(
            userNotificationReader
                .findAllByUserIdAndClubIdOrderByCreatedAtDesc(userId, clubId, pageable)
                .map(notificationMapper::toResponse),
        )
    }

    private companion object {
        const val MAX_PAGE_SIZE = 100
    }
}
