package com.weeth.domain.schedule.application.usecase.query

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.enums.AttendanceStatus
import com.weeth.domain.attendance.domain.repository.AttendanceReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.schedule.application.dto.response.EventResponse
import com.weeth.domain.schedule.application.dto.response.ScheduleAttendanceStatus
import com.weeth.domain.schedule.application.dto.response.ScheduleDetailResponse
import com.weeth.domain.schedule.application.dto.response.ScheduleResponse
import com.weeth.domain.schedule.application.exception.EventNotFoundException
import com.weeth.domain.schedule.application.mapper.EventMapper
import com.weeth.domain.schedule.application.mapper.ScheduleMapper
import com.weeth.domain.schedule.domain.enums.Type
import com.weeth.domain.schedule.domain.repository.EventRepository
import com.weeth.domain.session.application.exception.SessionNotFoundException
import com.weeth.domain.session.domain.entity.Session
import com.weeth.domain.session.domain.enums.SessionStatus
import com.weeth.domain.session.domain.repository.SessionReader
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class GetScheduleQueryService(
    private val eventRepository: EventRepository,
    private val sessionReader: SessionReader,
    private val attendanceReader: AttendanceReader,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val scheduleMapper: ScheduleMapper,
    private val eventMapper: EventMapper,
) {
    fun findEvent(
        clubId: Long,
        userId: Long,
        eventId: Long,
    ): EventResponse {
        clubMemberPolicy.getActiveMember(clubId, userId)
        val event = eventRepository.findByIdOrNull(eventId) ?: throw EventNotFoundException()

        if (event.club.id != clubId) throw EventNotFoundException()

        return eventMapper.toResponse(event)
    }

    fun findMonthly(
        clubId: Long,
        userId: Long,
        cardinal: Int,
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<ScheduleResponse> {
        clubMemberPolicy.getActiveMember(clubId, userId)

        val events =
            eventRepository
                .findByClubIdAndCardinalAndDateRange(clubId, cardinal, start, end)
                .map { scheduleMapper.toResponse(it) }

        val sessions =
            sessionReader
                .findAllByClubIdAndCardinalAndStartBetween(clubId, cardinal, start, end)
                .map { scheduleMapper.toResponse(it) }

        return (events + sessions).sortedBy { it.start }
    }

    fun findAdminEvents(
        clubId: Long,
        userId: Long,
        cardinal: Int?,
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<EventResponse> {
        clubMemberPolicy.getActiveMember(clubId, userId)
        val events =
            if (cardinal != null) {
                eventRepository.findByClubIdAndCardinalAndDateRange(clubId, cardinal, start, end)
            } else {
                eventRepository.findByClubIdAndDateRange(clubId, start, end)
            }
        return events.map { eventMapper.toResponse(it) }
    }

    fun findDetail(
        clubId: Long,
        userId: Long,
        id: Long,
        type: Type,
    ): ScheduleDetailResponse {
        clubMemberPolicy.getActiveMember(clubId, userId)
        return when (type) {
            Type.EVENT -> {
                val event = eventRepository.findByIdOrNull(id) ?: throw EventNotFoundException()
                if (event.club.id != clubId) throw EventNotFoundException()
                scheduleMapper.toDetailResponse(event)
            }

            Type.SESSION -> {
                val session = sessionReader.getById(id)
                if (session.club.id != clubId) throw SessionNotFoundException()
                val allAttendances = attendanceReader.findAllBySession(session)
                val myAttendance = attendanceReader.findBySessionAndUserId(session, userId)
                val myStatus = deriveAttendanceStatus(myAttendance, session)
                val attendedAt = myAttendance?.modifiedAt?.takeIf { myStatus == ScheduleAttendanceStatus.COMPLETED }
                scheduleMapper.toDetailResponse(session, allAttendances, myStatus, attendedAt)
            }
        }
    }

    private fun deriveAttendanceStatus(
        attendance: Attendance?,
        session: Session,
    ): ScheduleAttendanceStatus {
        val now = LocalDateTime.now()
        if (attendance == null) {
            return when {
                now.isBefore(session.start.minusMinutes(10)) -> ScheduleAttendanceStatus.UPCOMING
                session.status == SessionStatus.OPEN && session.isCheckInAllowed(now) -> ScheduleAttendanceStatus.OPEN
                else -> ScheduleAttendanceStatus.ABSENT
            }
        }
        return when (attendance.status) {
            AttendanceStatus.ATTEND -> {
                ScheduleAttendanceStatus.COMPLETED
            }

            AttendanceStatus.ABSENT -> {
                ScheduleAttendanceStatus.ABSENT
            }

            AttendanceStatus.PENDING -> {
                val isOpen =
                    session.status == SessionStatus.OPEN &&
                        !now.isBefore(session.start) &&
                        !now.isAfter(session.end)
                if (isOpen) ScheduleAttendanceStatus.OPEN else ScheduleAttendanceStatus.UPCOMING
            }
        }
    }

    fun findYearly(
        clubId: Long,
        userId: Long,
        year: Int,
    ): Map<Int, List<ScheduleResponse>> {
        clubMemberPolicy.getActiveMember(clubId, userId)

        val start = LocalDateTime.of(year, 1, 1, 0, 0)
        val end = LocalDateTime.of(year, 12, 31, 23, 59, 59)

        val events =
            eventRepository
                .findByClubIdAndStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(clubId, end, start)
                .map { scheduleMapper.toResponse(it) }

        val sessions =
            sessionReader
                .findAllByClubIdAndStartBetween(clubId, start, end)
                .map { scheduleMapper.toResponse(it) }

        return (events + sessions)
            .sortedBy { it.start }
            .flatMap { schedule ->
                (schedule.start.monthValue..schedule.end.monthValue).map { month -> month to schedule }
            }.groupBy({ it.first }, { it.second })
    }
}
