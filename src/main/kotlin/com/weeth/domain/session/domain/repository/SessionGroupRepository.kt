package com.weeth.domain.session.domain.repository

import com.weeth.domain.session.domain.entity.SessionGroup
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SessionGroupRepository : JpaRepository<SessionGroup, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sg FROM SessionGroup sg WHERE sg.id = :id")
    fun findByIdWithLock(
        @Param("id") id: Long,
    ): SessionGroup?
}
