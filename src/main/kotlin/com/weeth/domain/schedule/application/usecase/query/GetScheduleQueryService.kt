package com.weeth.domain.schedule.application.usecase.query

import com.weeth.domain.cardinal.domain.repository.CardinalReader
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
    private val cardinalReader: CardinalReader,
    private val scheduleMapper: ScheduleMapper,
    private val eventMapper: EventMapper,
) {
    // TODO(PR4): 해당 클럽 소속 멤버인지 검증 필요
    fun findEvent(
        clubId: Long,
        eventId: Long,
    ): EventResponse {
        val event = eventRepository.findByIdOrNull(eventId) ?: throw EventNotFoundException()
        if (clubId != 0L && event.club.id != clubId) throw EventNotFoundException()
        return eventMapper.toResponse(event)
    }

    // TODO(PR4): 해당 클럽 소속 멤버인지 검증 필요
    fun findMonthly(
        clubId: Long,
        start: LocalDateTime,
        end: LocalDateTime,
    ): List<ScheduleResponse> {
        val events =
            eventRepository
                .findByClubIdAndStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(clubId, end, start)
                .map { scheduleMapper.toResponse(it, false) }
        val sessions =
            sessionReader
                .findAllByClubIdAndStartBetween(clubId, start, end)
                .map { scheduleMapper.toResponse(it, true) }
        return (events + sessions).sortedBy { it.start }
    }

    // TODO(PR4): 해당 클럽 소속 멤버인지 검증 필요
    fun findYearly(
        clubId: Long,
        year: Int,
        semester: Int,
    ): Map<Int, List<ScheduleResponse>> {
        val cardinal = cardinalReader.getByYearAndSemester(year, semester)
        val events =
            eventRepository
                .findAllByClubIdAndCardinal(clubId, cardinal.cardinalNumber)
                .map { scheduleMapper.toResponse(it, false) }
        val sessions =
            sessionReader
                .findAllByClubIdAndCardinalIn(clubId, listOf(cardinal.cardinalNumber))
                .map { scheduleMapper.toResponse(it, true) }

        return (events + sessions)
            .sortedBy { it.start }
            .flatMap { schedule ->
                (schedule.start.monthValue..schedule.end.monthValue).map { month -> month to schedule }
            }.groupBy({ it.first }, { it.second })
    }
}
