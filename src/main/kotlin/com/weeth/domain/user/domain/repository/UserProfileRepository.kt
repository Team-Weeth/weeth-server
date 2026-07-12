package com.weeth.domain.user.domain.repository

import com.weeth.domain.user.domain.entity.UserProfile
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserProfileRepository : JpaRepository<UserProfile, Long> {
    fun findAllByUserIdOrderByIdAsc(userId: Long): List<UserProfile>

    fun findAllByUserIdAndIdIn(
        userId: Long,
        ids: List<Long>,
    ): List<UserProfile>

    fun findByIdAndUserId(
        id: Long,
        userId: Long,
    ): Optional<UserProfile>

    fun existsByIdAndUserId(
        id: Long,
        userId: Long,
    ): Boolean
}
