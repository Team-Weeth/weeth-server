package com.weeth.domain.notification.domain.repository

import com.weeth.domain.notification.domain.entity.NotificationQueueIncident
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationQueueIncidentRepository : JpaRepository<NotificationQueueIncident, String>
