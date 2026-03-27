package com.weeth.domain.schedule.application.usecase.query

import com.weeth.domain.cardinal.domain.repository.CardinalReader
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
    private val cardinalReader: CardinalReader,
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

    fun findYearly( // TODO: 기수가 1학기라는 보장이 없음. 기수 말고 날짜 기준으로 받아오기. (MVP 후)
        clubId: Long,
        userId: Long,
        year: Int,
        semester: Int,
    ): Map<Int, List<ScheduleResponse>> {
        clubMemberPolicy.getActiveMember(clubId, userId)
        val cardinal = cardinalReader.getByClubIdAndYearAndSemester(clubId, year, semester)

        val events =
            eventRepository
                .findAllByClubIdAndCardinal(clubId, cardinal.cardinalNumber)
                .map { scheduleMapper.toResponse(it) }

        val sessions =
            sessionReader
                .findAllByClubIdAndCardinalIn(clubId, listOf(cardinal.cardinalNumber))
                .map { scheduleMapper.toResponse(it) }

        return (events + sessions)
            .sortedBy { it.start }
            .flatMap { schedule ->
                (schedule.start.monthValue..schedule.end.monthValue).map { month -> month to schedule }
            }.groupBy({ it.first }, { it.second })
    }
}
