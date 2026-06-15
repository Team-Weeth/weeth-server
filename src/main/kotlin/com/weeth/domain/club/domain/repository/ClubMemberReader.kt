package com.weeth.domain.club.domain.repository

import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ClubMemberReader {
    fun findByIdWithLock(clubMemberId: Long): ClubMember?

    /**
     * 비관적 쓰기 락(PESSIMISTIC_WRITE)으로 여러 ClubMember를 조회한다.
     * 교착 방지를 위해 id 오름차순으로 락을 획득하며, 호출부에서도 [ids]를 정렬하여 전달해야 한다.
     */
    fun findAllByIdsWithLock(ids: List<Long>): List<ClubMember>

    fun findByIdOrNull(clubMemberId: Long): ClubMember?

    fun findByClubIdAndUserId(
        clubId: Long,
        userId: Long,
    ): ClubMember?

    fun findByClubIdAndUserIdWithLock(
        clubId: Long,
        userId: Long,
    ): ClubMember?

    fun findAllByClubId(clubId: Long): List<ClubMember>

    fun findAllByUserId(userId: Long): List<ClubMember>

    fun findAllByUserIdWithClub(userId: Long): List<ClubMember>

    fun findAllByUserIdAndMemberStatusWithClub(
        userId: Long,
        memberStatus: MemberStatus,
    ): List<ClubMember>

    fun findActiveByUserId(userId: Long): List<ClubMember>

    fun countActiveByClubId(clubId: Long): Long

    fun findActiveByClubIdAndKeyword(
        clubId: Long,
        keyword: String?,
        pageable: Pageable,
    ): Page<ClubMember>

    fun findExcludedPaymentTargetCandidates(
        clubId: Long,
        accountId: Long,
        keyword: String?,
        pageable: Pageable,
    ): Page<ClubMember>

    /** 특정 기수에 속한 활성 부원 수. 회비 납부 대상 후보는 동아리 전체가 아닌 해당 기수 명부로 한정한다. */
    fun countActiveByClubIdAndCardinalNumber(
        clubId: Long,
        cardinalNumber: Int,
    ): Long

    /** 특정 기수에 속한 활성 부원을 이름 검색·페이지네이션으로 조회한다. (납부 대상 전체 탭) */
    fun findActiveByClubIdAndCardinalNumberAndKeyword(
        clubId: Long,
        cardinalNumber: Int,
        keyword: String?,
        pageable: Pageable,
    ): Page<ClubMember>

    /** 특정 기수 명부 중 해당 장부에서 납부 대상(TARGETED)이 아닌 부원. (납부 대상 제외됨 탭) */
    fun findExcludedPaymentTargetCandidatesByCardinal(
        clubId: Long,
        cardinalNumber: Int,
        accountId: Long,
        keyword: String?,
        pageable: Pageable,
    ): Page<ClubMember>

    fun findAllByClubIdAndMemberStatus(
        clubId: Long,
        memberStatus: MemberStatus,
    ): List<ClubMember>

    fun countByUserIdAndMemberStatusAndMemberRole(
        userId: Long,
        memberStatus: MemberStatus,
        memberRole: MemberRole,
    ): Long

    fun findAllByClubIdAndUserIds(
        clubId: Long,
        userIds: List<Long>,
    ): List<ClubMember>
}
