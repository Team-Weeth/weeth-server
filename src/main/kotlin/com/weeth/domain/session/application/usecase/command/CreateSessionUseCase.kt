package com.weeth.domain.session.application.usecase.command

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberCardinalReader
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.session.application.dto.request.SessionCreateRequest
import com.weeth.domain.session.application.exception.RecurrenceEndDateBeforeStartException
import com.weeth.domain.session.application.exception.RecurrenceEndDateExceedsMaxException
import com.weeth.domain.session.application.exception.RecurrenceEndDateRequiredException
import com.weeth.domain.session.application.mapper.SessionMapper
import com.weeth.domain.session.domain.repository.SessionGroupRepository
import com.weeth.domain.session.domain.repository.SessionRepository
import com.weeth.domain.session.domain.service.RecurringSessionPolicy
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.repository.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CreateSessionUseCase(
    private val sessionRepository: SessionRepository,
    private val attendanceRepository: AttendanceRepository,
    private val sessionGroupRepository: SessionGroupRepository,
    private val userReader: UserReader,
    private val cardinalReader: CardinalReader,
    private val sessionMapper: SessionMapper,
    private val clubReader: ClubReader,
    private val clubMemberCardinalReader: ClubMemberCardinalReader,
    private val clubPermissionPolicy: ClubPermissionPolicy,
    private val recurringSessionPolicy: RecurringSessionPolicy,
) {
    @Transactional
    fun create(
        clubId: Long,
        request: SessionCreateRequest,
        userId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val club = clubReader.getClubById(clubId)
        val user = userReader.getById(userId)
        cardinalReader.findByClubIdAndCardinalNumber(clubId, request.cardinal) ?: throw CardinalNotFoundException()

        val members =
            clubMemberCardinalReader
                .findAllByClubIdAndCardinalNumber(clubId, request.cardinal, MemberStatus.ACTIVE)
                .map { it.clubMember }

        when (request.recurrenceType) {
            null -> createSingleSession(club, request, user, members)
            else -> createRecurringSessions(club, request, user, members)
        }
    }

    private fun createSingleSession(
        club: Club,
        request: SessionCreateRequest,
        user: User,
        members: List<ClubMember>,
    ) {
        val session = sessionMapper.toEntity(club, request, user)

        sessionRepository.save(session)
        attendanceRepository.saveAll(members.map { Attendance.create(session, it) })
    }

    /**
     * 반복 세션 생성 메서드
     */
    private fun createRecurringSessions(
        club: Club,
        request: SessionCreateRequest,
        user: User,
        members: List<ClubMember>,
    ) {
        val recurrenceType = checkNotNull(request.recurrenceType)
        val startDate = request.start.toLocalDate()
        val endDate =
            request.recurrenceEndDate
                ?: throw RecurrenceEndDateRequiredException()

        if (endDate.isBefore(startDate)) {
            throw RecurrenceEndDateBeforeStartException()
        }
        if (endDate.isAfter(startDate.plusYears(1))) {
            throw RecurrenceEndDateExceedsMaxException()
        }

        val schedules = recurringSessionPolicy.calculateSchedules(request.start, request.end, recurrenceType, endDate)
        if (schedules.isEmpty()) {
            throw RecurrenceEndDateBeforeStartException()
        }

        val group = sessionMapper.toSessionGroup(request, endDate)
        sessionGroupRepository.save(group)

        val sessions = sessionMapper.toEntities(club, request, user, group, schedules)
        sessionRepository.saveAll(sessions)

        val attendances = sessions.flatMap { session -> members.map { Attendance.create(session, it) } }
        attendanceRepository.saveAll(attendances)
    }
}
