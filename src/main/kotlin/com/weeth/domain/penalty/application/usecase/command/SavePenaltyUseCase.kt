package com.weeth.domain.penalty.application.usecase.command

import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.service.ClubMemberCardinalPolicy
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.penalty.application.dto.request.SavePenaltyRequest
import com.weeth.domain.penalty.application.exception.PenaltyNotFoundException
import com.weeth.domain.penalty.application.mapper.PenaltyMapper
import com.weeth.domain.penalty.domain.repository.PenaltyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SavePenaltyUseCase(
    private val penaltyRepository: PenaltyRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val clubPermissionPolicy: ClubPermissionPolicy,
    private val clubMemberCardinalPolicy: ClubMemberCardinalPolicy,
    private val mapper: PenaltyMapper,
) {
    @Transactional
    fun save(
        clubId: Long,
        userId: Long,
        request: SavePenaltyRequest,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val clubMember = clubMemberPolicy.getActiveMember(clubId, request.userId)
        val cardinal = clubMemberCardinalPolicy.getCurrentCardinal(clubMember)

        val penalty = mapper.toEntity(request, clubMember, cardinal)
        penaltyRepository.save(penalty)

        val lockedMember =
            clubMemberRepository.findByIdWithLock(clubMember.id)
                ?: throw PenaltyNotFoundException()
        lockedMember.incrementPenaltyCount()
    }
}
