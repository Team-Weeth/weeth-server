package com.weeth.domain.session.application.usecase.command

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.enums.AttendanceStatus
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.schedule.application.dto.request.ScheduleSaveRequest
import com.weeth.domain.schedule.application.dto.request.ScheduleUpdateRequest
import com.weeth.domain.schedule.application.mapper.SessionMapper
import com.weeth.domain.session.application.exception.SessionNotFoundException
import com.weeth.domain.session.domain.repository.SessionRepository
import com.weeth.domain.user.domain.repository.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * TODO: 출석 생성/삭제 관련해서 출석 초기화 로직과 함께 엣지 케이스나 개선 사항 점검 (join, applyOb)
 */
@Service
class ManageSessionUseCase(
    private val sessionRepository: SessionRepository,
    private val attendanceRepository: AttendanceRepository,
    private val userReader: UserReader,
    private val cardinalReader: CardinalReader,
    private val sessionMapper: SessionMapper,
    private val clubReader: ClubReader,
    private val clubMemberRepository: ClubMemberRepository,
    private val clubMemberPolicy: ClubMemberPolicy,
) {
    @Transactional
    fun create(
        clubId: Long,
        request: ScheduleSaveRequest,
        userId: Long,
    ) {
        clubMemberPolicy.requireAdmin(clubId, userId)
        val club = clubReader.getClubById(clubId)
        val user = userReader.getById(userId)
        cardinalReader.findByClubIdAndCardinalNumber(clubId, request.cardinal) ?: throw SessionNotFoundException()
        // TODO: 현재는 동아리 전체 ACTIVE 멤버에게 출석을 만든다. clubMemberCardinal 기준으로 좁히지 않으면 applyOb 초기화와 중복될 수 있다.
        val clubMembers = clubMemberRepository.findAllByClubIdAndMemberStatus(clubId, MemberStatus.ACTIVE)

        val session = sessionMapper.toEntity(club, request, user)
        sessionRepository.save(session)

        attendanceRepository.saveAll(clubMembers.map { Attendance.create(session, it) })
    }

    @Transactional
    fun update(
        clubId: Long,
        sessionId: Long,
        request: ScheduleUpdateRequest,
        userId: Long,
    ) {
        clubMemberPolicy.requireAdmin(clubId, userId)
        val session = sessionRepository.findByIdWithLock(sessionId) ?: throw SessionNotFoundException()
        if (session.club.id != clubId) throw SessionNotFoundException()
        val user = userReader.getById(userId)

        session.updateInfo(request.title, request.content, request.location, request.start, request.end, user)
    }

    @Transactional
    fun delete(
        clubId: Long,
        sessionId: Long,
        userId: Long,
    ) {
        clubMemberPolicy.requireAdmin(clubId, userId)
        val session = sessionRepository.findByIdWithLock(sessionId) ?: throw SessionNotFoundException()
        if (session.club.id != clubId) throw SessionNotFoundException()
        val attendances =
            attendanceRepository.findAllBySessionAndClubMemberMemberStatusWithLock(
                session,
                MemberStatus.ACTIVE,
            )

        attendances.forEach { a ->
            when (a.status) {
                AttendanceStatus.ATTEND -> a.clubMember.removeAttend()

                // 출석률 재계산은 내부에
                AttendanceStatus.ABSENT -> a.clubMember.removeAbsent()

                // 출석률 재계산은 내부에
                else -> Unit
            }
        }

        attendanceRepository.deleteAllBySession(session)
        sessionRepository.delete(session)
    }
}
