package com.weeth.domain.schedule.application.usecase.query

import com.weeth.domain.attendance.domain.repository.SessionRepository
import com.weeth.domain.schedule.application.dto.response.EventResponse
import com.weeth.domain.schedule.application.dto.response.ScheduleResponse
import com.weeth.domain.schedule.application.exception.EventNotFoundException
import com.weeth.domain.schedule.application.mapper.EventMapper
import com.weeth.domain.schedule.application.mapper.ScheduleMapper
import com.weeth.domain.schedule.domain.repository.EventRepository
import com.weeth.domain.user.domain.service.CardinalGetService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime


@Service
@Transactional(readOnly = true)
class GetScheduleQueryService(
    private val eventRepository: EventRepository,
    private val sessionRepository: SessionRepository,
    private val cardinalGetService: CardinalGetService,
    private val scheduleMapper: ScheduleMapper,
    private val eventMapper: EventMapper,
) {
    fun findEvent(eventId: Long): EventResponse =
        eventRepository.findByIdOrNull(eventId)
            ?.let { eventMapper.toResponse(it) }
            ?: throw EventNotFoundException()

    fun findMonthly(
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<ScheduleResponse> {
        val events =
            eventRepository
                .findByStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(end, start)
                .map { scheduleMapper.toResponse(it, false) }
        val sessions =
            sessionRepository
                .findByStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(end, start)
                .map { scheduleMapper.toResponse(it, true) }
        return (events + sessions).sortedBy { it.start }
    }

    fun findYearly(
        year: Int,
        semester: Int,
    ): Map<Int, List<ScheduleResponse>> {
        val cardinal = cardinalGetService.find(year, semester)
        val events =
            eventRepository
                .findAllByCardinal(cardinal.cardinalNumber)
                .map { scheduleMapper.toResponse(it, false) }
        val sessions =
            sessionRepository
                .findAllByCardinal(cardinal.cardinalNumber)
                .map { scheduleMapper.toResponse(it, true) }

        return (events + sessions)
            .sortedBy { it.start }
            .flatMap { schedule ->
                (schedule.start.monthValue..schedule.end.monthValue).map { month -> month to schedule }
            }.groupBy({ it.first }, { it.second })
    }
}
