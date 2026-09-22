package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.club.application.dto.request.SaveClubPositionOptionsRequest
import com.weeth.domain.club.application.exception.PositionOptionLimitExceededException
import com.weeth.domain.club.domain.entity.ClubPositionOption
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubPositionOptionRepository
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 동아리 멤버 관리 페이지에서 사용할 포지션(직책) 옵션 관리 유스케이스.
 * 요청받은 전체 목록으로 기존 옵션을 교체(upsert)한다 — 옵션이 없던 동아리는 새로 생기고,
 * 이미 있던 동아리는 전체가 교체된다.
 *
 * 요청 DTO에 id가 없어 name을 식별자로 매칭한다: 요청에 동일한 name이 남아있는 기존 옵션은
 * 같은 엔티티를 재사용(id 유지)해 그 옵션을 참조 중이던 ClubMember.positionOption을 보존하고,
 * name이 사라진 옵션만 삭제 대상이 되어 그 옵션을 참조하던 멤버의 참조만 정리(clear)한다.
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

        val club = clubReader.getClubById(clubId)
        val existingOptions = clubPositionOptionRepository.findAllByClubIdOrderByDisplayOrderAsc(clubId)
        val reusableByName = mutableMapOf<String, ArrayDeque<ClubPositionOption>>()
        existingOptions.forEach { option ->
            reusableByName.getOrPut(option.name) { ArrayDeque() }.addLast(option)
        }

        val newOptions = mutableListOf<ClubPositionOption>()
        request.options.forEachIndexed { index, optionRequest ->
            val reused = reusableByName[optionRequest.name]?.removeFirstOrNull()
            if (reused != null) {
                reused.update(optionRequest.name, optionRequest.color, index)
            } else {
                newOptions += ClubPositionOption.create(club, optionRequest.name, optionRequest.color, index)
            }
        }

        val removedOptionIds = reusableByName.values.flatten().map { it.id }
        if (removedOptionIds.isNotEmpty()) {
            clubMemberRepository.clearPositionOptionReferences(removedOptionIds)
            clubPositionOptionRepository.deleteAllByIdInBatch(removedOptionIds)
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
}
