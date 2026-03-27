package com.weeth.domain.cardinal.application.usecase.command

import com.weeth.domain.cardinal.application.dto.request.CardinalSaveRequest
import com.weeth.domain.cardinal.application.dto.request.CardinalUpdateRequest
import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.application.exception.DuplicateCardinalException
import com.weeth.domain.cardinal.application.mapper.CardinalMapper
import com.weeth.domain.cardinal.domain.repository.CardinalRepository
import com.weeth.domain.cardinal.domain.service.CardinalStatusPolicy
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManageCardinalUseCase(
    private val cardinalRepository: CardinalRepository,
    private val cardinalMapper: CardinalMapper,
    private val cardinalStatusPolicy: CardinalStatusPolicy,
    private val clubReader: ClubReader,
    private val clubPermissionPolicy: ClubPermissionPolicy,
) {
    @Transactional
    fun save(
        clubId: Long,
        request: CardinalSaveRequest,
        userId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val club = clubReader.getClubById(clubId)

        if (cardinalRepository.findByClubIdAndCardinalNumber(clubId, request.cardinalNumber) != null) {
            throw DuplicateCardinalException()
        }

        val cardinal = cardinalRepository.save(cardinalMapper.toEntity(club, request))

        if (request.inProgress) {
            cardinalStatusPolicy.activateExclusively(cardinal)
        }
    }

    @Transactional
    fun update(
        clubId: Long,
        request: CardinalUpdateRequest,
        userId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val cardinal =
            cardinalRepository.findByIdAndClubId(request.id, clubId) ?: throw CardinalNotFoundException()

        if (request.inProgress) {
            cardinalStatusPolicy.activateExclusively(cardinal)
        }
    }
}
