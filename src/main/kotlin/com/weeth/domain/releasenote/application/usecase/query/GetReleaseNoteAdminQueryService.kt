package com.weeth.domain.releasenote.application.usecase.query

import com.weeth.domain.releasenote.application.dto.response.ReleaseNoteResponse
import com.weeth.domain.releasenote.application.dto.response.ReleaseNoteSummaryResponse
import com.weeth.domain.releasenote.application.exception.ReleaseNoteNotFoundException
import com.weeth.domain.releasenote.application.mapper.ReleaseNoteMapper
import com.weeth.domain.releasenote.application.validator.PlatformPermissionValidator
import com.weeth.domain.releasenote.domain.enums.ReleaseNoteStatus
import com.weeth.domain.releasenote.domain.repository.ReleaseNoteRepository
import com.weeth.global.common.response.SliceResponse
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 백오피스 릴리즈 노트 조회(DRAFT 포함). DEVELOPER 권한을 요구한다.
 */
@Service
@Transactional(readOnly = true)
class GetReleaseNoteAdminQueryService(
    private val releaseNoteRepository: ReleaseNoteRepository,
    private val platformPermissionValidator: PlatformPermissionValidator,
    private val releaseNoteMapper: ReleaseNoteMapper,
) {
    fun findAll(
        userId: Long,
        status: ReleaseNoteStatus?,
        page: Int,
        size: Int,
    ): SliceResponse<ReleaseNoteSummaryResponse> {
        platformPermissionValidator.requireDeveloper(userId)
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, GetReleaseNoteQueryService.MAX_PAGE_SIZE))
        val slice = releaseNoteRepository.findForAdmin(status, pageable)
        return SliceResponse.from(slice.map(releaseNoteMapper::toSummaryResponse))
    }

    fun find(
        userId: Long,
        releaseNoteId: Long,
    ): ReleaseNoteResponse {
        platformPermissionValidator.requireDeveloper(userId)
        val note =
            releaseNoteRepository.findById(releaseNoteId).orElseThrow { ReleaseNoteNotFoundException() }
        return releaseNoteMapper.toResponse(note)
    }
}
