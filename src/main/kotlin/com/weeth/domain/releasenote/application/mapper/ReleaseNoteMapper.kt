package com.weeth.domain.releasenote.application.mapper

import com.weeth.domain.releasenote.application.dto.request.CreateReleaseNoteRequest
import com.weeth.domain.releasenote.application.dto.response.ReleaseNoteIdResponse
import com.weeth.domain.releasenote.application.dto.response.ReleaseNoteResponse
import com.weeth.domain.releasenote.application.dto.response.ReleaseNoteSummaryResponse
import com.weeth.domain.releasenote.domain.entity.ReleaseNote
import com.weeth.domain.releasenote.domain.service.ReleaseNoteSummaryExtractor
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ReleaseNoteMapper {
    fun toEntity(
        request: CreateReleaseNoteRequest,
        authorId: Long,
        now: LocalDateTime,
    ): ReleaseNote =
        ReleaseNote.create(
            title = request.title,
            content = request.content,
            thumbnailUrl = request.thumbnailUrl,
            releasedAt = request.releasedAt,
            authorId = authorId,
            status = request.status,
            now = now,
        )

    fun toResponse(releaseNote: ReleaseNote): ReleaseNoteResponse =
        ReleaseNoteResponse(
            id = releaseNote.id,
            title = releaseNote.title,
            thumbnailUrl = releaseNote.thumbnailUrl,
            content = releaseNote.content,
            status = releaseNote.status,
            releasedAt = releaseNote.releasedAt,
            publishedAt = releaseNote.publishedAt,
            createdAt = releaseNote.createdAt,
            updatedAt = releaseNote.modifiedAt,
            authorId = releaseNote.authorId,
        )

    fun toSummaryResponse(releaseNote: ReleaseNote): ReleaseNoteSummaryResponse =
        ReleaseNoteSummaryResponse(
            id = releaseNote.id,
            title = releaseNote.title,
            summary = ReleaseNoteSummaryExtractor.extract(releaseNote.content),
            thumbnailUrl = releaseNote.thumbnailUrl,
            status = releaseNote.status,
            releasedAt = releaseNote.releasedAt,
            publishedAt = releaseNote.publishedAt,
        )

    fun toIdResponse(id: Long): ReleaseNoteIdResponse = ReleaseNoteIdResponse(id = id)
}
