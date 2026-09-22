package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.club.application.dto.request.SaveClubPositionOptionsRequest
import com.weeth.domain.club.application.exception.PositionOptionLimitExceededException
import com.weeth.domain.club.application.exception.PositionOptionNotFoundException
import com.weeth.domain.club.application.exception.PositionOptionUpdateDeleteConflictException
import com.weeth.domain.club.domain.entity.ClubPositionOption
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubPositionOptionRepository
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 동아리 멤버 관리 페이지에서 사용할 포지션(직책) 옵션 관리 유스케이스.
 *
 * 요청의 `options`는 추가/수정만 표현한다 — `id`가 있으면 그 id의 기존 옵션을 재사용(update)해
 * ClubMember.positionOption 참조를 보존하고, `id`가 없으면 신규 생성한다.
 * 삭제는 `deletedPositionIds`로 명시적으로만 이루어지며, 그 옵션을 참조 중이던 멤버의 참조를
 * 먼저 정리(clear)한 뒤 삭제한다.
 */
@Service
class ManageClubPositionOptionUseCase(
    private val clubReader: ClubReader,
    private val clubPositionOptionRepository: ClubPositionOptionRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val clubPermissionPolicy: ClubPermissionPolicy,
) {
    @Transactional
    fun save(
        clubId: Long,
        userId: Long,
        request: SaveClubPositionOptionsRequest,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        validateOptionCount(request)
        validateNoUpdateDeleteConflict(request)

        val club = clubReader.getClubById(clubId)
        val existingById =
            clubPositionOptionRepository
                .findAllByClubIdOrderByDisplayOrderAsc(
                    clubId,
                ).associateBy { it.id }

        if (request.deletedPositionIds.isNotEmpty()) {
            request.deletedPositionIds.forEach { id -> existingById[id] ?: throw PositionOptionNotFoundException() }
            clubMemberRepository.clearPositionOptionReferences(request.deletedPositionIds)
            clubPositionOptionRepository.deleteAllByIdInBatch(request.deletedPositionIds)
        }

        val newOptions = mutableListOf<ClubPositionOption>()
        request.options.forEachIndexed { index, optionRequest ->
            if (optionRequest.id != null) {
                val existing = existingById[optionRequest.id] ?: throw PositionOptionNotFoundException()
                existing.update(optionRequest.name, optionRequest.color, index)
            } else {
                newOptions += ClubPositionOption.create(club, optionRequest.name, optionRequest.color, index)
            }
        }
        if (newOptions.isNotEmpty()) {
            clubPositionOptionRepository.saveAll(newOptions)
        }
    }

    private fun validateOptionCount(request: SaveClubPositionOptionsRequest) {
        if (request.options.size > ClubPositionOption.MAX_OPTION_COUNT) {
            throw PositionOptionLimitExceededException()
        }
    }

    private fun validateNoUpdateDeleteConflict(request: SaveClubPositionOptionsRequest) {
        val updatedIds = request.options.mapNotNullTo(mutableSetOf()) { it.id }
        if (updatedIds.any { it in request.deletedPositionIds }) {
            throw PositionOptionUpdateDeleteConflictException()
        }
    }
}
