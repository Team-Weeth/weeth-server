package com.weeth.domain.account.domain.repository

import com.weeth.domain.account.domain.entity.AccountTransaction
import com.weeth.domain.account.domain.enums.AccountTransactionDirection
import com.weeth.domain.account.domain.enums.AccountTransactionType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface AccountTransactionRepository : JpaRepository<AccountTransaction, Long> {
    fun countByAccountIdAndTypeInAndDeletedAtIsNull(
        accountId: Long,
        types: Collection<AccountTransactionType>,
    ): Long

    fun findByIdAndDeletedAtIsNull(id: Long): AccountTransaction?

    /** 대시보드 월별 집계용. 삭제 제외 전체 거래를 거래일 오름차순으로 조회한다. */
    fun findByAccountIdAndDeletedAtIsNullOrderByTransactedAtAsc(accountId: Long): List<AccountTransaction>

    /** 대시보드 period 종료월 계산용. 다음 기수 장부의 활동 시작(가장 이른 거래)을 찾는다. */
    fun findTopByAccountIdAndDeletedAtIsNullOrderByTransactedAtAsc(accountId: Long): AccountTransaction?

    /** 납부 정정(unpaid) 시 해당 납부 대상의 활성 시스템 거래(DUES)를 찾아 원복한다. */
    fun findByPaymentTargetIdAndTypeAndDeletedAtIsNull(
        paymentTargetId: Long,
        type: AccountTransactionType,
    ): AccountTransaction?

    // 목록 조회 (소프트 삭제 제외)
    fun findByAccountIdAndDeletedAtIsNull(
        accountId: Long,
        pageable: Pageable,
    ): Page<AccountTransaction>

    fun findByAccountIdAndTypeAndDeletedAtIsNull(
        accountId: Long,
        type: AccountTransactionType,
        pageable: Pageable,
    ): Page<AccountTransaction>

    fun findByAccountIdAndTypeInAndDeletedAtIsNull(
        accountId: Long,
        types: Collection<AccountTransactionType>,
        pageable: Pageable,
    ): Page<AccountTransaction>

    fun findByAccountIdAndDirectionAndDeletedAtIsNull(
        accountId: Long,
        direction: AccountTransactionDirection,
        pageable: Pageable,
    ): Page<AccountTransaction>

    // 필터 탭 카운트
    fun countByAccountIdAndDeletedAtIsNull(accountId: Long): Long

    fun countByAccountIdAndTypeAndDeletedAtIsNull(
        accountId: Long,
        type: AccountTransactionType,
    ): Long

    fun countByAccountIdAndDirectionAndDeletedAtIsNull(
        accountId: Long,
        direction: AccountTransactionDirection,
    ): Long
}
