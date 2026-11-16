package com.weeth.domain.releasenote.application.mapper

import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.releasenote.application.dto.request.CreateReleaseNoteRequest
import com.weeth.domain.releasenote.application.dto.response.ReleaseNoteIdResponse
import com.weeth.domain.releasenote.application.dto.response.ReleaseNoteResponse
import com.weeth.domain.releasenote.application.dto.response.ReleaseNoteSummaryResponse
import com.weeth.domain.releasenote.domain.entity.ReleaseNote
import com.weeth.domain.releasenote.domain.service.ReleaseNoteSummaryExtractor
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ReleaseNoteMapper(
    private val fileAccessUrlPort: FileAccessUrlPort,
) {
    fun toEntity(
        request: CreateReleaseNoteRequest,
        authorId: Long,
        now: LocalDateTime,
    ): ReleaseNote =
        ReleaseNote.create(
            title = request.title,
            content = request.content,
            thumbnailStorageKey = request.thumbnail?.storageKey,
            releasedAt = request.releasedAt,
            authorId = authorId,
            status = request.status,
            now = now,
        )

    fun toResponse(releaseNote: ReleaseNote): ReleaseNoteResponse =
        ReleaseNoteResponse(
            id = releaseNote.id,
            title = releaseNote.title,
            thumbnailUrl = resolveThumbnail(releaseNote.thumbnailStorageKey),
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
            thumbnailUrl = resolveThumbnail(releaseNote.thumbnailStorageKey),
            status = releaseNote.status,
            releasedAt = releaseNote.releasedAt,
            publishedAt = releaseNote.publishedAt,
        )

    fun toIdResponse(id: Long): ReleaseNoteIdResponse = ReleaseNoteIdResponse(id = id)

    // 저장된 storageKey를 조회용 URL로 변환한다(설정에 따라 CDN URL 적용).
    private fun resolveThumbnail(storageKey: String?): String? = storageKey?.let(fileAccessUrlPort::resolve)
}
