package com.weeth.domain.notification.domain.repository

import com.weeth.domain.notification.domain.entity.NotificationToken
import com.weeth.domain.notification.domain.vo.PushTarget

interface NotificationTokenReader {
    fun findByToken(token: String): NotificationToken?

    fun findActiveTargetsByUserIds(userIds: List<Long>): List<PushTarget>
}
