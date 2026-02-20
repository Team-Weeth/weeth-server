package com.weeth.domain.schedule.application.usecase.command

import com.weeth.domain.schedule.application.dto.request.ScheduleSaveRequest
import com.weeth.domain.schedule.application.dto.request.ScheduleUpdateRequest
import com.weeth.domain.schedule.application.exception.EventNotFoundException
import com.weeth.domain.schedule.application.mapper.EventMapper
import com.weeth.domain.schedule.domain.repository.EventRepository
import com.weeth.domain.user.domain.service.CardinalGetService
import com.weeth.domain.user.domain.service.UserGetService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManageEventUseCase(
    private val eventRepository: EventRepository,
    private val userGetService: UserGetService,
    private val cardinalGetService: CardinalGetService,
    private val eventMapper: EventMapper,
) {
    @Transactional
    fun create(
        request: ScheduleSaveRequest,
        userId: Long,
    ) {
        val user = userGetService.find(userId)
        cardinalGetService.findByUserSide(request.cardinal)
        eventRepository.save(eventMapper.toEntity(request, user))
    }

    @Transactional
    fun update(
        eventId: Long,
        request: ScheduleUpdateRequest,
        userId: Long,
    ) {
        val user = userGetService.find(userId)
        val event = eventRepository.findByIdOrNull(eventId) ?: throw EventNotFoundException()
        event.update(request.title, request.content, request.location, request.start, request.end, user)
    }

    @Transactional
    fun delete(eventId: Long) {
        val event = eventRepository.findByIdOrNull(eventId) ?: throw EventNotFoundException()
        eventRepository.delete(event)
    }
}
