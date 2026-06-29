package com.weeth.domain.account.domain.repository

import com.weeth.domain.account.domain.entity.AccountPaymentTarget
import com.weeth.domain.account.domain.enums.AccountPaymentStatus
import com.weeth.domain.account.domain.enums.AccountTargetStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AccountPaymentTargetRepository : JpaRepository<AccountPaymentTarget, Long> {
    fun findAllByAccountId(accountId: Long): List<AccountPaymentTarget>

    @Query(
        """
        select target
        from AccountPaymentTarget target
        join fetch target.clubMember clubMember
        join fetch clubMember.user
        where target.account.id = :accountId
        order by target.id asc
        """,
    )
    fun findAllByAccountIdOrderByIdAsc(
        @Param("accountId") accountId: Long,
    ): List<AccountPaymentTarget>

    @Query(
        """
        select target
        from AccountPaymentTarget target
        join fetch target.clubMember clubMember
        join fetch clubMember.user
        where target.account.id = :accountId
        and clubMember.id in :clubMemberIds
        """,
    )
    fun findAllByAccountIdAndClubMemberIdIn(
        @Param("accountId") accountId: Long,
        @Param("clubMemberIds") clubMemberIds: List<Long>,
    ): List<AccountPaymentTarget>

    /** 납부/환불 벌크 액션 대상. 거래 내용(거래처)에 멤버 이름을 쓰기 위해 user 까지 fetch 한다. */
    @Query(
        """
        select target
        from AccountPaymentTarget target
        join fetch target.clubMember clubMember
        join fetch clubMember.user
        where target.account.id = :accountId
        and target.id in :ids
        """,
    )
    fun findAllByAccountIdAndIdIn(
        @Param("accountId") accountId: Long,
        @Param("ids") ids: List<Long>,
    ): List<AccountPaymentTarget>

    @Query(
        """
        select count(target)
        from AccountPaymentTarget target
        join target.clubMember clubMember
        where target.account.id = :accountId
        and target.targetStatus = :targetStatus
        and clubMember.memberStatus = com.weeth.domain.club.domain.enums.MemberStatus.ACTIVE
        """,
    )
    fun countActiveClubMemberTargetsByAccountIdAndTargetStatus(
        @Param("accountId") accountId: Long,
        @Param("targetStatus") targetStatus: AccountTargetStatus,
    ): Long

    @Query(
        value = """
        select target
        from AccountPaymentTarget target
        join fetch target.clubMember clubMember
        join fetch clubMember.user user
        where target.account.id = :accountId
        and target.targetStatus = :targetStatus
        and clubMember.memberStatus = com.weeth.domain.club.domain.enums.MemberStatus.ACTIVE
        and (:keyword is null or user.name like concat('%', :keyword, '%'))
        order by target.id asc
        """,
        countQuery = """
        select count(target)
        from AccountPaymentTarget target
        join target.clubMember clubMember
        join clubMember.user user
        where target.account.id = :accountId
        and target.targetStatus = :targetStatus
        and clubMember.memberStatus = com.weeth.domain.club.domain.enums.MemberStatus.ACTIVE
        and (:keyword is null or user.name like concat('%', :keyword, '%'))
        """,
    )
    fun findAllActiveClubMemberTargetsByAccountIdAndTargetStatus(
        @Param("accountId") accountId: Long,
        @Param("targetStatus") targetStatus: AccountTargetStatus,
        @Param("keyword") keyword: String?,
        pageable: Pageable,
    ): Page<AccountPaymentTarget>

    /**
     * 운영 납부현황 화면용. 납부 대상(TARGETED) 부원을 납부 상태 필터·이름 검색으로 조회하고
     * 미납(UNPAID) 우선, 그다음 이름 오름차순으로 정렬한다.
     */
    @Query(
        value = """
        select target
        from AccountPaymentTarget target
        join fetch target.clubMember clubMember
        join fetch clubMember.user user
        where target.account.id = :accountId
        and target.targetStatus = com.weeth.domain.account.domain.enums.AccountTargetStatus.TARGETED
        and clubMember.memberStatus = com.weeth.domain.club.domain.enums.MemberStatus.ACTIVE
        and (:paymentStatus is null or target.paymentStatus = :paymentStatus)
        and (:keyword is null or user.name like concat('%', :keyword, '%'))
        order by
            case when target.paymentStatus = com.weeth.domain.account.domain.enums.AccountPaymentStatus.UNPAID
                then 0 else 1 end asc,
            user.name asc,
            target.id asc
        """,
        countQuery = """
        select count(target)
        from AccountPaymentTarget target
        join target.clubMember clubMember
        join clubMember.user user
        where target.account.id = :accountId
        and target.targetStatus = com.weeth.domain.account.domain.enums.AccountTargetStatus.TARGETED
        and clubMember.memberStatus = com.weeth.domain.club.domain.enums.MemberStatus.ACTIVE
        and (:paymentStatus is null or target.paymentStatus = :paymentStatus)
        and (:keyword is null or user.name like concat('%', :keyword, '%'))
        """,
    )
    fun findActiveTargetsByPaymentStatusOrderByUnpaidFirst(
        @Param("accountId") accountId: Long,
        @Param("paymentStatus") paymentStatus: AccountPaymentStatus?,
        @Param("keyword") keyword: String?,
        pageable: Pageable,
    ): Page<AccountPaymentTarget>

    /** 총 수납액: 납부 완료(PAID) 대상의 paidAmount 합. */
    @Query(
        """
        select coalesce(sum(target.paidAmount), 0)
        from AccountPaymentTarget target
        where target.account.id = :accountId
        and target.targetStatus = com.weeth.domain.account.domain.enums.AccountTargetStatus.TARGETED
        and target.paymentStatus = com.weeth.domain.account.domain.enums.AccountPaymentStatus.PAID
        """,
    )
    fun sumPaidAmountByAccountId(
        @Param("accountId") accountId: Long,
    ): Long

    /** 목표액: 납부 대상(TARGETED) dueAmount 합. */
    @Query(
        """
        select coalesce(sum(target.dueAmount), 0)
        from AccountPaymentTarget target
        where target.account.id = :accountId
        and target.targetStatus = com.weeth.domain.account.domain.enums.AccountTargetStatus.TARGETED
        """,
    )
    fun sumDueAmountByAccountId(
        @Param("accountId") accountId: Long,
    ): Long

    /** 초안 폐기 시 장부에 딸린 납부 대상 행을 일괄 삭제한다. (FK에 cascade가 없어 장부 삭제 전에 호출해야 한다) */
    @Modifying
    @Query("delete from AccountPaymentTarget target where target.account.id = :accountId")
    fun deleteAllByAccountId(
        @Param("accountId") accountId: Long,
    )

    /** 탈퇴/퇴출 등으로 비활성화된 멤버의 미납 납부 대상 행. 등록 완료 시 제외 처리 대상이다. */
    @Query(
        """
        select target
        from AccountPaymentTarget target
        join fetch target.clubMember clubMember
        where target.account.id = :accountId
        and target.targetStatus = com.weeth.domain.account.domain.enums.AccountTargetStatus.TARGETED
        and target.paymentStatus = com.weeth.domain.account.domain.enums.AccountPaymentStatus.UNPAID
        and clubMember.memberStatus <> com.weeth.domain.club.domain.enums.MemberStatus.ACTIVE
        """,
    )
    fun findAllUnpaidTargetsWithInactiveClubMemberByAccountId(
        @Param("accountId") accountId: Long,
    ): List<AccountPaymentTarget>

    fun countByAccountIdAndTargetStatus(
        accountId: Long,
        targetStatus: AccountTargetStatus,
    ): Long

    fun countByAccountIdAndTargetStatusAndPaymentStatus(
        accountId: Long,
        targetStatus: AccountTargetStatus,
        paymentStatus: AccountPaymentStatus,
    ): Long
}
