package com.weeth.domain.account.domain.repository

import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.enums.AccountStatus
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param

interface AccountRepository : JpaRepository<Account, Long> {
    fun findByClubIdAndCardinal(
        clubId: Long,
        cardinal: Int,
    ): Account?

    fun existsByClubIdAndCardinal(
        clubId: Long,
        cardinal: Int,
    ): Boolean

    fun findByClubIdAndCardinalAndStatus(
        clubId: Long,
        cardinal: Int,
        status: AccountStatus,
    ): Account?

    fun existsByClubIdAndCardinalAndStatus(
        clubId: Long,
        cardinal: Int,
        status: AccountStatus,
    ): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("select a from Account a where a.id = :id")
    fun findByIdWithLock(
        @Param("id") id: Long,
    ): Account?
}
