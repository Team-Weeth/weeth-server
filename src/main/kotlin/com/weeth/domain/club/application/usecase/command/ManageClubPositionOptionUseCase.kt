package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.club.application.dto.request.SaveClubPositionOptionsRequest
import com.weeth.domain.club.application.exception.PositionOptionLimitExceededException
import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.entity.ClubPositionOption
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubPositionOptionRepository
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.domain.vo.ColorHex
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 동아리 멤버 관리 페이지에서 사용할 포지션(직책) 옵션 관리 유스케이스.
 * 요청받은 전체 목록으로 기존 옵션을 완전히 교체(upsert)한다 — 옵션이 없던 동아리는 새로 생기고,
 * 이미 있던 동아리는 전체가 교체된다.
 *
 * 주의: 교체는 id-diff가 아닌 hard delete 후 재생성이므로, 이름/색만 바뀌는 옵션도 새 id를 받는다.
 * 따라서 save() 호출 시점에 존재하던 옵션을 참조 중인 ClubMember.positionOption은 항상 끊어지므로,
 * 삭제 전에 반드시 해당 멤버들의 참조를 먼저 정리(clear)한다.
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

        val existingOptionIds = clubPositionOptionRepository.findAllByClubIdOrderByDisplayOrderAsc(clubId).map { it.id }
        if (existingOptionIds.isNotEmpty()) {
            clubMemberRepository.clearPositionOptionReferences(existingOptionIds)
        }

        clubPositionOptionRepository.hardDeleteAllByClubId(clubId)
        clubPositionOptionRepository.saveAll(buildPositionOptions(club, request))
    }

    private fun validateOptionCount(request: SaveClubPositionOptionsRequest) {
        if (request.options.size > ClubPositionOption.MAX_OPTION_COUNT) {
            throw PositionOptionLimitExceededException()
        }
    }

    private fun buildPositionOptions(
        club: Club,
        request: SaveClubPositionOptionsRequest,
    ): List<ClubPositionOption> =
        request.options.mapIndexed { index, option ->
            ClubPositionOption.create(
                club = club,
                name = option.name,
                colorHex = ColorHex(option.colorHex).value,
                displayOrder = index,
            )
        }
}
