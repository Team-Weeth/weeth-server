package com.weeth.domain.session.domain.repository

import com.weeth.domain.session.domain.entity.SessionGroup
import org.springframework.data.jpa.repository.JpaRepository

interface SessionGroupRepository : JpaRepository<SessionGroup, Long>
