package com.weeth.domain.schedule.application.usecase.command

import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.schedule.application.dto.request.ScheduleSaveRequest
import com.weeth.domain.schedule.application.dto.request.ScheduleUpdateRequest
import com.weeth.domain.schedule.application.exception.EventNotFoundException
import com.weeth.domain.schedule.application.mapper.EventMapper
import com.weeth.domain.schedule.domain.repository.EventRepository
import com.weeth.domain.user.domain.repository.UserReader
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManageEventUseCase(
    private val eventRepository: EventRepository,
    private val userReader: UserReader,
    private val cardinalReader: CardinalReader,
    private val eventMapper: EventMapper,
    private val clubReader: ClubReader,
) {
    // TODO(PR4): 해당 클럽 소속 admin인지 검증 필요
    @Transactional
    fun create(
        clubId: Long,
        request: ScheduleSaveRequest,
        userId: Long,
    ) {
        val club = clubReader.getClubById(clubId)
        val user = userReader.getById(userId)
        cardinalReader.getByCardinalNumber(request.cardinal)
        eventRepository.save(eventMapper.toEntity(club, request, user))
    }

    // TODO(PR4): 해당 클럽 소속 admin인지 검증 필요
    @Transactional
    fun update(
        clubId: Long,
        eventId: Long,
        request: ScheduleUpdateRequest,
        userId: Long,
    ) {
        val user = userReader.getById(userId)
        val event = eventRepository.findByIdOrNull(eventId) ?: throw EventNotFoundException()
        event.update(request.title, request.content, request.location, request.start, request.end, user)
    }

    // TODO(PR4): 해당 클럽 소속 admin인지 검증 필요
    @Transactional
    fun delete(
        clubId: Long,
        eventId: Long,
    ) {
        val event = eventRepository.findByIdOrNull(eventId) ?: throw EventNotFoundException()
        eventRepository.delete(event)
    }
}
