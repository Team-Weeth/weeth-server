package com.weeth.domain.notification.domain.repository

import com.weeth.domain.notification.domain.entity.NotificationFailure
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationFailureRepository : JpaRepository<NotificationFailure, Long>
