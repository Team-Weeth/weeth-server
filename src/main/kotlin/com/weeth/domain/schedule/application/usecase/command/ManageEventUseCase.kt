package com.weeth.domain.schedule.application.usecase.command

import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
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
    private val clubPermissionPolicy: ClubPermissionPolicy,
) {
    @Transactional
    fun create(
        clubId: Long,
        request: ScheduleSaveRequest,
        userId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val club = clubReader.getClubById(clubId)
        val user = userReader.getById(userId)
        // TODO: 전역 cardinal 조회 대신 clubId 기준 조회를 사용해야 다른 동아리 기수로 검증이 통과하지 않는다.
        cardinalReader.getByCardinalNumber(request.cardinal)
        eventRepository.save(eventMapper.toEntity(club, request, user))
    }

    @Transactional
    fun update(
        clubId: Long,
        eventId: Long,
        request: ScheduleUpdateRequest,
        userId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val user = userReader.getById(userId)
        val event = eventRepository.findByIdOrNull(eventId) ?: throw EventNotFoundException()
        if (event.club.id != clubId) throw EventNotFoundException()
        event.update(request.title, request.content, request.location, request.start, request.end, user)
    }

    @Transactional
    fun delete(
        clubId: Long,
        eventId: Long,
        userId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val event = eventRepository.findByIdOrNull(eventId) ?: throw EventNotFoundException()
        if (event.club.id != clubId) throw EventNotFoundException()
        eventRepository.delete(event)
    }
}
