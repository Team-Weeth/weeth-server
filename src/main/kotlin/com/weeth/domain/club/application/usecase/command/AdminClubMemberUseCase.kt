package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.enums.AttendanceStatus
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.club.application.dto.request.ClubMemberApplyObRequest
import com.weeth.domain.club.application.dto.request.ClubMemberRoleUpdateRequest
import com.weeth.domain.club.application.dto.request.UpdateMemberCardinalRequest
import com.weeth.domain.club.application.exception.CardinalRemovalHasAttendanceException
import com.weeth.domain.club.application.exception.ClubMemberNotFoundException
import com.weeth.domain.club.application.exception.ClubMemberNotInClubException
import com.weeth.domain.club.application.exception.LeadSelfTransferException
import com.weeth.domain.club.application.exception.LeadTransferOnlyException
import com.weeth.domain.club.application.exception.NotLeadException
import com.weeth.domain.club.application.exception.SelfBanNotAllowedException
import com.weeth.domain.club.application.exception.SelfRoleChangeNotAllowedException
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
        val adminMember = clubPermissionPolicy.requireAdmin(clubId, userId)

        val member = clubMemberPolicy.getMemberInClub(clubId, clubMemberId)
        if (adminMember.id == member.id) throw SelfBanNotAllowedException()
        member.ban()
    }

    @Transactional
    fun restore(
        clubId: Long,
        userId: Long,
        clubMemberId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val member = clubMemberPolicy.getMemberInClub(clubId, clubMemberId)
        member.restore()
    }

    @Transactional
    fun updateMemberRole(
        clubId: Long,
        userId: Long,
        clubMemberId: Long,
        request: ClubMemberRoleUpdateRequest,
    ) {
        val adminMember = clubPermissionPolicy.requireAdmin(clubId, userId)

        val member = clubMemberPolicy.getMemberInClub(clubId, clubMemberId)
        if (request.memberRole == MemberRole.LEAD) throw LeadTransferOnlyException()
        if (adminMember.id == member.id) throw SelfRoleChangeNotAllowedException()
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
        val attendanceInitMap = linkedMapOf<ClubMember, MutableList<Cardinal>>()

        uniqueRequests.forEach { request ->
            val member = memberMap[request.clubMemberId] ?: throw ClubMemberNotFoundException()
            val nextCardinal =
                cardinalByNumber.getOrPut(request.cardinal) {
                    cardinalReader.findByClubIdAndCardinalNumber(clubId, request.cardinal)
                        ?: throw CardinalNotFoundException()
                }

            if (clubMemberCardinalPolicy.notContains(member, nextCardinal)) {
                if (clubMemberCardinalPolicy.isLatestOrFirstCardinal(member, nextCardinal)) {
                    member.resetAttendanceStats()
                    member.resetPenaltyCount()
                    attendanceInitMap.getOrPut(member) { mutableListOf() }.add(nextCardinal)
                }

                clubMemberCardinalRepository.save(ClubMemberCardinal.create(member, nextCardinal))
            }
        }

        attendanceInitMap.forEach { (member, cardinals) ->
            initializeAttendances(clubId, member, cardinals)
        }
    }

    @Transactional
    fun updateCardinals(
        clubId: Long,
        userId: Long,
        clubMemberId: Long,
        request: UpdateMemberCardinalRequest,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val member =
            clubMemberReader.findByIdWithLock(clubMemberId)
                ?: throw ClubMemberNotFoundException()
        if (member.club.id != clubId) throw ClubMemberNotInClubException()

        val distinctIds = request.cardinalIds.distinct()
        val newCardinals = cardinalReader.findAllByClubIdAndIdIn(clubId, distinctIds)
        if (newCardinals.size != distinctIds.size) throw CardinalNotFoundException()

        val currentMemberCardinals = clubMemberCardinalRepository.findAllByClubMembers(listOf(member))
        val existingCardinalIds = currentMemberCardinals.map { it.cardinal.id }.toSet()
        val newCardinalIds = newCardinals.map { it.id }.toSet()

        val toRemove = currentMemberCardinals.filter { it.cardinal.id !in newCardinalIds }
        if (toRemove.isNotEmpty()) {
            val removedCardinalNumbers = toRemove.map { it.cardinal.cardinalNumber }
            val sessions = sessionReader.findAllByClubIdAndCardinalIn(clubId, removedCardinalNumbers)
            if (sessions.isNotEmpty()) {
                val attendances = attendanceRepository.findAllByClubMemberAndSessionIn(member, sessions)

                // force=true면 강제 삭제, 아니면 클라이언트에 확인 요청
                val hasRecord =
                    attendances.any {
                        it.status == AttendanceStatus.ATTEND || it.status == AttendanceStatus.ABSENT
                    }
                if (!request.force && hasRecord) {
                    throw CardinalRemovalHasAttendanceException()
                }

                attendanceRepository.deleteAll(attendances)

                val latestCardinal = newCardinals.maxByOrNull { it.cardinalNumber }
                if (latestCardinal != null) {
                    val remaining =
                        attendanceRepository.findAllByClubMemberIdAndCardinal(
                            member.id,
                            latestCardinal.cardinalNumber,
                        )
                    val attendCount = remaining.count { it.status == AttendanceStatus.ATTEND }
                    val absentCount = remaining.count { it.status == AttendanceStatus.ABSENT }
                    member.recalculateAttendanceStats(attendCount, absentCount)
                } else {
                    member.recalculateAttendanceStats(0, 0)
                }
            }
            clubMemberCardinalRepository.deleteAll(toRemove)
        }

        val toAdd = newCardinals.filter { it.id !in existingCardinalIds }
        if (toAdd.isNotEmpty()) {
            clubMemberCardinalRepository.saveAll(toAdd.map { ClubMemberCardinal.create(member, it) })
            initializeAttendances(clubId, member, toAdd)
        }
    }

    // TODO: ManageClubMemberUsecase.initializeAttendances와 중복 — MVP 후 공통 서비스로 추출
    private fun initializeAttendances(
        clubId: Long,
        member: ClubMember,
        cardinals: List<Cardinal>,
    ) {
        val sessions = sessionReader.findAllByClubIdAndCardinalIn(clubId, cardinals.map { it.cardinalNumber })
        if (sessions.isEmpty()) return

        attendanceRepository.saveAll(sessions.map { Attendance.create(session = it, clubMember = member) })
    }
}
