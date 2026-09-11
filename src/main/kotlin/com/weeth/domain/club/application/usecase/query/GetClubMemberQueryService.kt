package com.weeth.domain.club.application.usecase.query

import com.weeth.domain.board.domain.repository.PostReader
import com.weeth.domain.club.application.dto.request.ClubMemberSort
import com.weeth.domain.club.application.dto.response.ClubMemberDetailResponse
import com.weeth.domain.club.application.dto.response.ClubMemberProfileResponse
import com.weeth.domain.club.application.dto.response.ClubMemberPublicResponse
import com.weeth.domain.club.application.dto.response.ClubMemberResponse
import com.weeth.domain.club.application.dto.response.ClubMemberSummaryResponse
import com.weeth.domain.club.application.dto.response.ProfileStatusResponse
import com.weeth.domain.club.application.exception.ClubMemberNotFoundException
import com.weeth.domain.club.application.exception.ClubMemberNotInClubException
import com.weeth.domain.club.application.mapper.ClubMapper
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.repository.ClubMemberCardinalReader
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.penalty.domain.repository.PenaltyReader
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.global.common.response.PageResponse
import com.weeth.global.common.response.SliceResponse
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetClubMemberQueryService(
    private val clubMemberReader: ClubMemberReader,
    private val clubMemberCardinalReader: ClubMemberCardinalReader,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val clubPermissionPolicy: ClubPermissionPolicy,
    private val clubMapper: ClubMapper,
    private val userReader: UserReader,
    private val penaltyReader: PenaltyReader,
    private val postReader: PostReader,
) {
    fun findClubMembersForAdmin(
        clubId: Long,
        userId: Long,
        page: Int,
        size: Int,
        keyword: String?,
        cardinalNumber: Int?,
        sort: ClubMemberSort,
    ): PageResponse<ClubMemberResponse> {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, MAX_PAGE_SIZE))
        val members =
            clubMemberReader.findAdminMembers(
                clubId = clubId,
                cardinalNumber = cardinalNumber,
                keyword = keyword?.trim()?.takeIf { it.isNotBlank() },
                sortKey = sort.queryKey,
                pageable = pageable,
            )

        // 기수와 최근 페널티는 조회된 페이지의 멤버에 대해서만 일괄 조회해 N+1을 피한다.
        val clubMemberIds = members.content.map { it.id }
        val cardinalsByMemberId =
            if (members.isEmpty) {
                emptyMap()
            } else {
                clubMemberCardinalReader.findAllByClubMembers(members.content).groupBy { it.clubMember.id }
            }
        val lastPenaltyAtByMemberId =
            if (clubMemberIds.isEmpty()) {
                emptyMap()
            } else {
                penaltyReader
                    .findByClubMemberIds(clubMemberIds)
                    .groupBy { it.clubMember.id }
                    .mapValues { (_, penalties) -> penalties.first().createdAt }
            }

        return PageResponse.from(
            members.map { member ->
                clubMapper.toMemberResponse(
                    member,
                    cardinalsByMemberId[member.id] ?: emptyList(),
                    lastPenaltyAtByMemberId[member.id],
                )
            },
        )
    }

    /**
     * 어드민 멤버 상세. 승인 대기·추방·탈퇴 멤버도 관리 화면에서 볼 수 있어야 하므로
     * 활성 멤버만 조회하는 [findMyMemberProfile]과 분리한다.
     */
    fun findClubMemberDetailForAdmin(
        clubId: Long,
        userId: Long,
        clubMemberId: Long,
    ): ClubMemberResponse {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val member = clubMemberReader.findAdminMemberDetail(clubMemberId) ?: throw ClubMemberNotFoundException()
        if (member.club.id != clubId) throw ClubMemberNotInClubException()

        val cardinals = clubMemberCardinalReader.findAllByClubMember(member)
        return clubMapper.toMemberResponse(member, cardinals)
    }

    fun findMyMemberProfile(
        clubId: Long,
        userId: Long,
    ): ClubMemberProfileResponse {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)
        val cardinals = clubMemberCardinalReader.findAllByClubMember(member)

        return clubMapper.toMemberProfileResponse(member, cardinals)
    }

    fun findProfileStatus(
        clubId: Long,
        userId: Long,
    ): ProfileStatusResponse {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)
        val user = userReader.getById(userId)
        val cardinalAssigned = clubMemberCardinalReader.findLatestCardinalByClubMember(member) != null

        return clubMapper.toProfileStatusResponse(user, cardinalAssigned)
    }

    fun findMySummary(
        clubId: Long,
        userId: Long,
    ): ClubMemberSummaryResponse {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)
        val cardinals = clubMemberCardinalReader.findAllByClubMember(member)

        return clubMapper.toMemberSummaryResponse(member, cardinals)
    }

    fun searchClubMembers(
        clubId: Long,
        userId: Long,
        keyword: String,
        cardinalNumber: Int?,
    ): List<ClubMemberResponse> =
        findClubMembersForAdmin(
            clubId = clubId,
            userId = userId,
            page = 0,
            size = MAX_SEARCH_SIZE,
            keyword = keyword,
            cardinalNumber = cardinalNumber,
            sort = ClubMemberSort.CARDINAL_DESC, // 검색은 기수 내림차순 고정
        ).content

    fun findMemberDetail(
        clubId: Long,
        userId: Long,
        clubMemberId: Long,
    ): ClubMemberDetailResponse {
        clubMemberPolicy.getActiveMember(clubId, userId)

        val member =
            clubMemberReader.findPublicMemberDetail(clubId, clubMemberId)
                ?: throw ClubMemberNotFoundException()

        val cardinals = clubMemberCardinalReader.findAllByClubMember(member)
        val postCount = postReader.countActiveByClubMemberIds(listOf(clubMemberId))

        return clubMapper.toMemberDetailResponse(member, cardinals, postCount)
    }

    fun findPublicMembers(
        clubId: Long,
        userId: Long,
        cardinalNumber: Int?,
        memberRole: MemberRole?,
        keyword: String?,
        page: Int,
        size: Int,
    ): SliceResponse<ClubMemberPublicResponse> {
        clubMemberPolicy.getActiveMember(clubId, userId)

        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, MAX_PAGE_SIZE))
        val members =
            clubMemberReader.findPublicMembers(
                clubId = clubId,
                cardinalNumber = cardinalNumber,
                memberRole = memberRole,
                keyword = keyword?.trim()?.takeIf { it.isNotBlank() },
                pageable = pageable,
            )

        val cardinalsByMemberId =
            if (members.isEmpty) {
                emptyMap()
            } else {
                clubMemberCardinalReader.findAllByClubMembers(members.content).groupBy { it.clubMember.id }
            }

        return SliceResponse.from(
            members.map { member ->
                clubMapper.toPublicMemberResponse(member, cardinalsByMemberId[member.id] ?: emptyList())
            },
        )
    }

    companion object {
        private const val MAX_PAGE_SIZE = 100
        private const val MAX_SEARCH_SIZE = 50
    }
}
