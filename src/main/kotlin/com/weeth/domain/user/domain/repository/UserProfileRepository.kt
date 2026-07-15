package com.weeth.domain.user.domain.repository

import com.weeth.domain.user.domain.entity.UserProfile
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import java.util.Optional

interface UserProfileRepository : JpaRepository<UserProfile, Long> {
    fun findAllByUserIdOrderByIdAsc(userId: Long): List<UserProfile>

    fun findAllByUserIdAndIdIn(
        userId: Long,
        ids: List<Long>,
    ): List<UserProfile>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query(
        """
        SELECT up
        FROM UserProfile up
        WHERE up.user.id = :userId
        AND up.id IN :ids
        ORDER BY up.id ASC
        """,
    )
    fun findAllByUserIdAndIdInWithLock(
        @Param("userId") userId: Long,
        @Param("ids") ids: List<Long>,
    ): List<UserProfile>

    fun findByIdAndUserId(
        id: Long,
        userId: Long,
    ): Optional<UserProfile>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query(
        """
        SELECT up
        FROM UserProfile up
        WHERE up.id = :id
        AND up.user.id = :userId
        """,
    )
    fun findByIdAndUserIdWithLock(
        @Param("id") id: Long,
        @Param("userId") userId: Long,
    ): Optional<UserProfile>

    fun existsByIdAndUserId(
        id: Long,
        userId: Long,
    ): Boolean
}
