package com.weeth.domain.releasenote.application.usecase.query

import com.weeth.domain.releasenote.application.dto.response.ReleaseNoteResponse
import com.weeth.domain.releasenote.application.dto.response.ReleaseNoteSummaryResponse
import com.weeth.domain.releasenote.application.exception.ReleaseNoteNotFoundException
import com.weeth.domain.releasenote.application.mapper.ReleaseNoteMapper
import com.weeth.domain.releasenote.domain.enums.ReleaseNoteStatus
import com.weeth.domain.releasenote.domain.repository.ReleaseNoteRepository
import com.weeth.global.common.response.SliceResponse
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 공개(비로그인 포함) 릴리즈 노트 조회. 항상 PUBLISHED만 반환하며 DRAFT는 어떤 경우에도 노출하지 않는다.
 */
@Service
@Transactional(readOnly = true)
class GetReleaseNoteQueryService(
    private val releaseNoteRepository: ReleaseNoteRepository,
    private val releaseNoteMapper: ReleaseNoteMapper,
) {
    fun findAll(
        keyword: String?,
        page: Int,
        size: Int,
    ): SliceResponse<ReleaseNoteSummaryResponse> {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, MAX_PAGE_SIZE))
        val slice =
            releaseNoteRepository.findPublic(
                status = ReleaseNoteStatus.PUBLISHED,
                keyword = keyword?.trim().orEmpty(),
                pageable = pageable,
            )
        return SliceResponse.from(slice.map(releaseNoteMapper::toSummaryResponse))
    }

    fun find(releaseNoteId: Long): ReleaseNoteResponse {
        val note =
            releaseNoteRepository.findByIdAndStatus(releaseNoteId, ReleaseNoteStatus.PUBLISHED)
                ?: throw ReleaseNoteNotFoundException()
        return releaseNoteMapper.toResponse(note)
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 12
        const val MAX_PAGE_SIZE = 100
    }
}
