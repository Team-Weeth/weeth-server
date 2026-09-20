package com.weeth.domain.notification.application.usecase.command

import com.weeth.domain.notification.application.dto.request.RegisterNotificationTokenRequest
import com.weeth.domain.notification.application.dto.request.RevokeNotificationTokenRequest
import com.weeth.domain.notification.domain.entity.NotificationToken
import com.weeth.domain.notification.domain.repository.NotificationTokenRepository
import com.weeth.domain.user.domain.repository.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ManageNotificationTokenUseCase(
    private val userReader: UserReader,
    private val notificationTokenRepository: NotificationTokenRepository,
) {
    @Transactional
    fun register(
        userId: Long,
        request: RegisterNotificationTokenRequest,
    ) {
        val user = userReader.getById(userId)
        notificationTokenRepository.registerToken(
            userId = user.id,
            token = NotificationToken.normalizeToken(request.token),
            registeredAt = LocalDateTime.now(),
        )
    }

    @Transactional
    fun revoke(
        userId: Long,
        request: RevokeNotificationTokenRequest,
    ) {
        notificationTokenRepository.deactivateByUserIdAndToken(
            userId = userId,
            token = NotificationToken.normalizeToken(request.token),
        )
    }
}
