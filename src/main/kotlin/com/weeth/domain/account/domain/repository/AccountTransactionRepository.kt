package com.weeth.domain.account.domain.repository

import com.weeth.domain.account.domain.entity.AccountTransaction
import com.weeth.domain.account.domain.enums.AccountTransactionDirection
import com.weeth.domain.account.domain.enums.AccountTransactionType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

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

    // 무한 스크롤: 전체 개수를 세지 않고 size+1 조회로 다음 페이지 존재 여부만 판별하므로 countQuery 가 없다.
    @Query(
        """
        select transaction
        from AccountTransaction transaction
        left join transaction.paymentTarget paymentTarget
        left join paymentTarget.clubMember clubMember
        where transaction.account.id = :accountId
        and transaction.deletedAt is null
        and (
            transaction.type in :publicTypes
            or (
                :includeRefund = true
                and transaction.type = com.weeth.domain.account.domain.enums.AccountTransactionType.REFUND
                and clubMember.id = :clubMemberId
            )
        )
        """,
    )
    fun findMemberVisibleTransactions(
        @Param("accountId") accountId: Long,
        @Param("clubMemberId") clubMemberId: Long,
        @Param("publicTypes") publicTypes: Collection<AccountTransactionType>,
        @Param("includeRefund") includeRefund: Boolean,
        pageable: Pageable,
    ): Slice<AccountTransaction>

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

    @Query(
        """
        select count(transaction)
        from AccountTransaction transaction
        left join transaction.paymentTarget paymentTarget
        left join paymentTarget.clubMember clubMember
        where transaction.account.id = :accountId
        and transaction.deletedAt is null
        and (
            transaction.type in :publicTypes
            or (
                :includeRefund = true
                and transaction.type = com.weeth.domain.account.domain.enums.AccountTransactionType.REFUND
                and clubMember.id = :clubMemberId
            )
        )
        """,
    )
    fun countMemberVisibleTransactions(
        @Param("accountId") accountId: Long,
        @Param("clubMemberId") clubMemberId: Long,
        @Param("publicTypes") publicTypes: Collection<AccountTransactionType>,
        @Param("includeRefund") includeRefund: Boolean,
    ): Long

    // 회비 집계는 환불을 净차감한다: DUES 는 보존하고 REFUND 지출 거래를 별도로 쌓는 모델이라
    // DUES 합만 보면 환불된 금액까지 포함되므로, REFUND 금액을 빼서 실제 순납부액을 반환한다.
    @Query(
        """
        select coalesce(
            sum(
                case
                    when transaction.type = com.weeth.domain.account.domain.enums.AccountTransactionType.DUES
                        then transaction.amount
                    when transaction.type = com.weeth.domain.account.domain.enums.AccountTransactionType.REFUND
                        then -transaction.amount
                    else 0
                end
            ),
            0
        )
        from AccountTransaction transaction
        where transaction.account.id = :accountId
        and transaction.deletedAt is null
        and transaction.type in (
            com.weeth.domain.account.domain.enums.AccountTransactionType.DUES,
            com.weeth.domain.account.domain.enums.AccountTransactionType.REFUND
        )
        """,
    )
    fun sumNetDuesAmountByAccountId(
        @Param("accountId") accountId: Long,
    ): Long
}
