package com.weeth.domain.releasenote.application.usecase.command

import com.weeth.domain.releasenote.application.dto.request.CreateReleaseNoteRequest
import com.weeth.domain.releasenote.application.dto.request.UpdateReleaseNoteRequest
import com.weeth.domain.releasenote.application.dto.request.UpdateReleaseNoteStatusRequest
import com.weeth.domain.releasenote.application.dto.response.ReleaseNoteIdResponse
import com.weeth.domain.releasenote.application.exception.ReleaseNoteNotFoundException
import com.weeth.domain.releasenote.application.mapper.ReleaseNoteMapper
import com.weeth.domain.releasenote.application.validator.PlatformPermissionValidator
import com.weeth.domain.releasenote.domain.entity.ReleaseNote
import com.weeth.domain.releasenote.domain.repository.ReleaseNoteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 릴리즈 노트 백오피스 명령(작성/수정/상태변경/삭제). 모든 동작은 DEVELOPER 권한을 요구한다.
 */
@Service
class ManageReleaseNoteUseCase(
    private val releaseNoteRepository: ReleaseNoteRepository,
    private val platformPermissionValidator: PlatformPermissionValidator,
    private val releaseNoteMapper: ReleaseNoteMapper,
) {
    @Transactional
    fun create(
        userId: Long,
        request: CreateReleaseNoteRequest,
    ): ReleaseNoteIdResponse {
        platformPermissionValidator.requireDeveloper(userId)
        val saved =
            releaseNoteRepository.save(
                releaseNoteMapper.toEntity(request, authorId = userId, now = LocalDateTime.now()),
            )
        return releaseNoteMapper.toIdResponse(saved.id)
    }

    @Transactional
    fun update(
        userId: Long,
        releaseNoteId: Long,
        request: UpdateReleaseNoteRequest,
    ) {
        platformPermissionValidator.requireDeveloper(userId)
        getOrThrow(releaseNoteId).update(
            title = request.title,
            content = request.content,
            thumbnailStorageKey = request.thumbnail?.storageKey,
            releasedAt = request.releasedAt,
        )
    }

    @Transactional
    fun updateStatus(
        userId: Long,
        releaseNoteId: Long,
        request: UpdateReleaseNoteStatusRequest,
    ) {
        platformPermissionValidator.requireDeveloper(userId)
        getOrThrow(releaseNoteId).changeStatus(request.status, LocalDateTime.now())
    }

    @Transactional
    fun delete(
        userId: Long,
        releaseNoteId: Long,
    ) {
        platformPermissionValidator.requireDeveloper(userId)
        releaseNoteRepository.delete(getOrThrow(releaseNoteId))
    }

    private fun getOrThrow(releaseNoteId: Long): ReleaseNote =
        releaseNoteRepository.findById(releaseNoteId).orElseThrow { ReleaseNoteNotFoundException() }
}
