package com.weeth.domain.notification.domain.repository

import com.weeth.domain.notification.domain.entity.NotificationToken

interface NotificationTokenReader {
    fun findByToken(token: String): NotificationToken?

    fun findActiveTokensByUserIds(userIds: List<Long>): List<String>
}
