package com.weeth.domain.club.application.usecase.query

import com.weeth.domain.board.domain.repository.PostReader
import com.weeth.domain.club.application.dto.request.ClubMemberSort
import com.weeth.domain.club.application.dto.response.ClubMemberDetailResponse
import com.weeth.domain.club.application.dto.response.ClubMemberProfileResponse
import com.weeth.domain.club.application.dto.response.ClubMemberPublicResponse
import com.weeth.domain.club.application.dto.response.ClubMemberResponse
import com.weeth.domain.club.application.dto.response.ClubMemberSummaryResponse
import com.weeth.domain.club.application.dto.response.ClubPositionOptionResponse
import com.weeth.domain.club.application.dto.response.ProfileStatusResponse
import com.weeth.domain.club.application.exception.ClubMemberNotFoundException
import com.weeth.domain.club.application.exception.ClubMemberNotInClubException
import com.weeth.domain.club.application.mapper.ClubMapper
import com.weeth.domain.club.application.mapper.ClubPositionOptionMapper
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.repository.ClubMemberCardinalReader
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.repository.ClubPositionOptionReader
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
    private val clubPositionOptionMapper: ClubPositionOptionMapper,
    private val clubPositionOptionReader: ClubPositionOptionReader,
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
        memberRole: MemberRole?,
        sort: ClubMemberSort,
    ): PageResponse<ClubMemberResponse> {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, MAX_PAGE_SIZE))
        val members =
            clubMemberReader.findAdminMembers(
                clubId = clubId,
                cardinalNumber = cardinalNumber,
                memberRole = memberRole,
                keyword = keyword?.trim()?.takeIf { it.isNotBlank() },
                sortKey = sort.queryKey,
                pageable = pageable,
            )

        // 기수, 최근 페널티, 포지션은 조회된 페이지의 멤버에 대해서만 일괄 조회해 N+1을 피한다.
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
        val positionResponseByOptionId = loadPositionResponsesByOptionId(members.content)

        return PageResponse.from(
            members.map { member ->
                clubMapper.toMemberResponse(
                    member,
                    cardinalsByMemberId[member.id] ?: emptyList(),
                    lastPenaltyAtByMemberId[member.id],
                    member.positionOption?.id?.let { positionResponseByOptionId[it] },
                )
            },
        )
    }

    /**
     * 페이지 내 멤버들의 positionOption id를 모아 일괄 조회한다.
     * 매퍼가 member.positionOption.name/color를 행별로 직접 역참조하면 지연 로딩으로 N+1이 재도입되므로,
     * 반드시 이 맵을 통해 미리 조회된 응답을 넘겨야 한다.
     */
    private fun loadPositionResponsesByOptionId(members: List<ClubMember>): Map<Long, ClubPositionOptionResponse> {
        val optionIds = members.mapNotNull { it.positionOption?.id }.distinct()
        if (optionIds.isEmpty()) return emptyMap()

        return clubPositionOptionReader
            .findAllByIdIn(optionIds)
            .associate { it.id to clubPositionOptionMapper.toResponse(it) }
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
        val position = member.positionOption?.let { clubPositionOptionMapper.toResponse(it) }
        return clubMapper.toMemberResponse(member, cardinals, position = position)
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

    /**
     * keyword가 역할 라벨("부원"/"리더"/"운영진")과 정확히 일치하면 이름/학과/학번 검색 대신
     * 해당 역할로만 필터링한다. 그 외에는 기존과 동일한 이름/학과/학번 부분일치 검색으로 동작한다.
     */
    fun searchClubMembers(
        clubId: Long,
        userId: Long,
        keyword: String?,
        cardinalNumber: Int?,
    ): List<ClubMemberResponse> {
        val trimmedKeyword = keyword?.trim()?.takeIf { it.isNotBlank() }
        val roleFromKeyword = trimmedKeyword?.let { ROLE_LABELS[it] }

        return findClubMembersForAdmin(
            clubId = clubId,
            userId = userId,
            page = 0,
            size = MAX_SEARCH_SIZE,
            keyword = if (roleFromKeyword != null) null else trimmedKeyword,
            cardinalNumber = cardinalNumber,
            memberRole = roleFromKeyword,
            sort = ClubMemberSort.CARDINAL_DESC, // 검색은 기수 내림차순 고정
        ).content
    }

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
        val position = member.positionOption?.let { clubPositionOptionMapper.toResponse(it) }

        return clubMapper.toMemberDetailResponse(member, cardinals, postCount, position)
    }

    fun findPublicMembers(
        clubId: Long,
        userId: Long,
        cardinalNumber: Int?,
        memberRole: MemberRole?,
        keyword: String?,
        positionOptionId: Long?,
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
                positionOptionId = positionOptionId,
                pageable = pageable,
            )

        val cardinalsByMemberId =
            if (members.isEmpty) {
                emptyMap()
            } else {
                clubMemberCardinalReader.findAllByClubMembers(members.content).groupBy { it.clubMember.id }
            }
        val positionResponseByOptionId = loadPositionResponsesByOptionId(members.content)

        return SliceResponse.from(
            members.map { member ->
                clubMapper.toPublicMemberResponse(
                    member,
                    cardinalsByMemberId[member.id] ?: emptyList(),
                    member.positionOption?.id?.let { positionResponseByOptionId[it] },
                )
            },
        )
    }

    companion object {
        private const val MAX_PAGE_SIZE = 100
        private const val MAX_SEARCH_SIZE = 50
        private val ROLE_LABELS =
            mapOf(
                "부원" to MemberRole.USER,
                "리더" to MemberRole.LEAD,
                "운영진" to MemberRole.ADMIN,
            )
    }
}
