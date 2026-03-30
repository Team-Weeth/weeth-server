package com.weeth.domain.schedule.application.usecase.query

import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.schedule.application.dto.response.EventResponse
import com.weeth.domain.schedule.application.dto.response.ScheduleResponse
import com.weeth.domain.schedule.application.exception.EventNotFoundException
import com.weeth.domain.schedule.application.mapper.EventMapper
import com.weeth.domain.schedule.application.mapper.ScheduleMapper
import com.weeth.domain.schedule.domain.repository.EventRepository
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
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<ScheduleResponse> {
        clubMemberPolicy.getActiveMember(clubId, userId)

        val events =
            eventRepository
                .findByClubIdAndStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(clubId, end, start)
                .map { scheduleMapper.toResponse(it) }

        val sessions =
            sessionReader
                .findAllByClubIdAndStartBetween(clubId, start, end)
                .map { scheduleMapper.toResponse(it) }

        return (events + sessions).sortedBy { it.start }
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
