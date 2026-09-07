package com.weeth.domain.penalty.application.usecase.command

import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubMemberCardinalPolicy
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.penalty.application.dto.request.SavePenaltyRequest
import com.weeth.domain.penalty.application.exception.PenaltyNotFoundException
import com.weeth.domain.penalty.application.exception.WarningNotEnabledException
import com.weeth.domain.penalty.application.mapper.PenaltyMapper
import com.weeth.domain.penalty.domain.enums.PenaltyType
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
    private val clubReader: ClubReader,
    private val mapper: PenaltyMapper,
) {
    @Transactional
    fun save(
        clubId: Long,
        userId: Long,
        request: SavePenaltyRequest,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        if (request.penaltyType == PenaltyType.WARNING) {
            val club = clubReader.getClubById(clubId)
            if (!club.warningEnabled) throw WarningNotEnabledException()
        }

        // TODO: 현재 userIds 수만큼 쿼리가 발생하는 N+1 문제가 있음. bulk 조회 방식으로 리팩토링 필요
        request.userIds.forEach { targetUserId ->
            val clubMember = clubMemberPolicy.getActiveMember(clubId, targetUserId)
            val cardinal = clubMemberCardinalPolicy.getCurrentCardinal(clubMember)

            val penalty = mapper.toEntity(request, clubMember, cardinal)
            penaltyRepository.save(penalty)

            val lockedMember =
                clubMemberRepository.findByIdWithLock(clubMember.id)
                    ?: throw PenaltyNotFoundException()
            when (penalty.penaltyType) {
                PenaltyType.PENALTY -> lockedMember.incrementPenaltyCount(request.score)
                PenaltyType.WARNING -> lockedMember.incrementWarningCount(request.score)
            }
        }
    }
}
