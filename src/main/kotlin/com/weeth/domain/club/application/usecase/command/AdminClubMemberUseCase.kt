package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.club.application.dto.request.ClubMemberApplyObRequest
import com.weeth.domain.club.application.dto.request.ClubMemberRoleUpdateRequest
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.repository.ClubMemberCardinalRepository
import com.weeth.domain.club.domain.service.ClubMemberCardinalPolicy
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.session.domain.repository.SessionReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 동아리 관리자 전용 멤버 관리 UseCase
 */
@Service
class AdminClubMemberUseCase(
    private val clubMemberPolicy: ClubMemberPolicy,
    private val clubMemberCardinalPolicy: ClubMemberCardinalPolicy,
    private val cardinalReader: CardinalReader,
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
        clubMemberPolicy.requireAdmin(clubId, userId)

        val member = clubMemberPolicy.getMemberInClub(clubId, clubMemberId)
        member.accept()
    }

    @Transactional
    fun ban(
        clubId: Long,
        userId: Long,
        clubMemberId: Long,
    ) {
        clubMemberPolicy.requireAdmin(clubId, userId)

        val member = clubMemberPolicy.getMemberInClub(clubId, clubMemberId)
        member.ban()
    }

    @Transactional
    fun updateMemberRole(
        clubId: Long,
        userId: Long,
        request: ClubMemberRoleUpdateRequest,
    ) {
        clubMemberPolicy.requireAdmin(clubId, userId)

        val member = clubMemberPolicy.getMemberInClub(clubId, request.clubMemberId)
        member.updateRole(request.memberRole)
    }

    @Transactional
    fun applyOb(
        clubId: Long,
        userId: Long,
        requests: List<ClubMemberApplyObRequest>,
    ) {
        clubMemberPolicy.requireAdmin(clubId, userId)

        val uniqueRequests = requests.distinctBy { it.clubMemberId to it.cardinal }
        if (uniqueRequests.isEmpty()) return

        val cardinalByNumber = mutableMapOf<Int, Cardinal>()

        uniqueRequests.forEach { request ->
            val member = clubMemberPolicy.getMemberInClub(clubId, request.clubMemberId)
            val nextCardinal =
                cardinalByNumber.getOrPut(request.cardinal) {
                    cardinalReader.findByClubIdAndCardinalNumber(clubId, request.cardinal)
                        ?: throw CardinalNotFoundException()
                }

            if (clubMemberCardinalPolicy.notContains(member, nextCardinal)) {
                if (clubMemberCardinalPolicy.isLatestOrFirstCardinal(member, nextCardinal)) {
                    member.resetAttendanceStats()
                    initializeAttendances(clubId, member, nextCardinal)
                }

                clubMemberCardinalRepository.save(ClubMemberCardinal.create(member, nextCardinal))
            }
        }
    }

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
