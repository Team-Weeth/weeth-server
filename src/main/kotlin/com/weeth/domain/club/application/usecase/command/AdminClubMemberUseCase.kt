package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.club.application.dto.request.ClubMemberApplyObRequest
import com.weeth.domain.club.application.dto.request.ClubMemberRoleUpdateRequest
import com.weeth.domain.club.application.exception.ClubMemberNotFoundException
import com.weeth.domain.club.application.exception.ClubMemberNotInClubException
import com.weeth.domain.club.application.exception.LeadSelfTransferException
import com.weeth.domain.club.application.exception.LeadTransferOnlyException
import com.weeth.domain.club.application.exception.NotLeadException
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.repository.ClubMemberCardinalRepository
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.service.ClubMemberCardinalPolicy
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.session.domain.repository.SessionReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 동아리 관리자 전용 멤버 관리 UseCase
 */
@Service
class AdminClubMemberUseCase(
    private val clubMemberPolicy: ClubMemberPolicy,
    private val clubPermissionPolicy: ClubPermissionPolicy,
    private val clubMemberCardinalPolicy: ClubMemberCardinalPolicy,
    private val cardinalReader: CardinalReader,
    private val clubMemberReader: ClubMemberReader,
    private val sessionReader: SessionReader,
    private val attendanceRepository: AttendanceRepository,
    private val clubMemberCardinalRepository: ClubMemberCardinalRepository,
) {
    @Transactional
    fun accept(
        clubId: Long,
        userId: Long,
        clubMemberId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val member = clubMemberPolicy.getMemberInClub(clubId, clubMemberId)
        member.accept()
    }

    @Transactional
    fun ban(
        clubId: Long,
        userId: Long,
        clubMemberId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val member = clubMemberPolicy.getMemberInClub(clubId, clubMemberId)
        member.ban()
    }

    @Transactional
    fun updateMemberRole(
        clubId: Long,
        userId: Long,
        request: ClubMemberRoleUpdateRequest,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val member = clubMemberPolicy.getMemberInClub(clubId, request.clubMemberId)
        if (request.memberRole == MemberRole.LEAD) throw LeadTransferOnlyException()
        if (member.isLead()) throw LeadTransferOnlyException()
        member.updateRole(request.memberRole)
    }

    @Transactional
    fun transferLead(
        clubId: Long,
        userId: Long,
        targetClubMemberId: Long,
    ) {
        val currentLead = clubMemberPolicy.getActiveMemberWithLock(clubId, userId)
        if (!currentLead.isLead()) throw NotLeadException()

        val target = clubMemberPolicy.getActiveMemberInClubWithLock(clubId, targetClubMemberId)
        if (currentLead.id == target.id) throw LeadSelfTransferException()

        currentLead.releaseLead()
        target.assignLead()
    }

    // TODO: setInitialCardinals와 동시 호출 시 출석 중복 생성 가능 — 멤버 단위 락 추가 검토
    @Transactional
    fun applyOb(
        clubId: Long,
        userId: Long,
        requests: List<ClubMemberApplyObRequest>,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val uniqueRequests = requests.distinctBy { it.clubMemberId to it.cardinal }
        if (uniqueRequests.isEmpty()) return

        val memberIds = uniqueRequests.map { it.clubMemberId }.distinct().sorted()
        val memberMap =
            clubMemberReader
                .findAllByIdsWithLock(memberIds)
                .also { members ->
                    if (members.any { it.club.id != clubId }) throw ClubMemberNotInClubException()
                }.associateBy { it.id }

        val cardinalByNumber = mutableMapOf<Int, Cardinal>()

        uniqueRequests.forEach { request ->
            val member = memberMap[request.clubMemberId] ?: throw ClubMemberNotFoundException()
            val nextCardinal =
                cardinalByNumber.getOrPut(request.cardinal) {
                    cardinalReader.findByClubIdAndCardinalNumber(clubId, request.cardinal)
                        ?: throw CardinalNotFoundException()
                }

            if (clubMemberCardinalPolicy.notContains(member, nextCardinal)) {
                if (clubMemberCardinalPolicy.isLatestOrFirstCardinal(member, nextCardinal)) {
                    member.resetAttendanceStats() // TODO: 페널티 카운트도 초기화
                    initializeAttendances(clubId, member, nextCardinal)
                }

                clubMemberCardinalRepository.save(ClubMemberCardinal.create(member, nextCardinal))
            }
        }
    }

    // TODO: ManageClubMemberUsecase.initializeAttendances와 중복 — MVP 후 공통 서비스로 추출
    private fun initializeAttendances(
        clubId: Long,
        member: ClubMember,
        cardinal: Cardinal,
    ) {
        val sessions = sessionReader.findAllByClubIdAndCardinalIn(clubId, listOf(cardinal.cardinalNumber))
        if (sessions.isEmpty()) return

        val attendances = sessions.map { Attendance.create(session = it, clubMember = member) }
        attendanceRepository.saveAll(attendances)
    }
}
