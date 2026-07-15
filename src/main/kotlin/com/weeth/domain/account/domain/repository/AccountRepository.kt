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

    fun findAllByClubIdAndStatusOrderByCardinalDesc(
        clubId: Long,
        status: AccountStatus,
    ): List<Account>

    fun findTopByClubIdAndCardinalLessThanAndStatusOrderByCardinalDesc(
        clubId: Long,
        cardinal: Int,
        status: AccountStatus,
    ): Account?

    /** 대시보드 period 종료월 계산용. 현재 기수의 "다음 회비 장부"(가장 가까운 상위 기수)를 찾는다. */
    fun findTopByClubIdAndCardinalGreaterThanAndStatusOrderByCardinalAsc(
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
