package com.weeth.domain.file.domain.entity

import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.enums.FileStatus
import com.weeth.domain.file.domain.vo.FileContentType
import com.weeth.domain.file.domain.vo.StorageKey
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "file",
    indexes = [
        Index(name = "idx_file_owner_type_owner_id", columnList = "owner_type, owner_id"),
    ],
)
class File(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,
    @Column(nullable = false)
    var fileName: String,
    @Column(nullable = false, length = 500, unique = true)
    val storageKey: StorageKey,
    @Column(nullable = false)
    val fileSize: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val ownerType: FileOwnerType,
    @Column(nullable = false)
    val ownerId: Long,
    @Column(nullable = false, length = 100)
    val contentType: FileContentType,
    // TODO: 하드 딜리트로 전환 완료되어 더 이상 사용되지 않음. DB 마이그레이션 후 status 컬럼 및 FileStatus enum 제거 예정
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: FileStatus = FileStatus.UPLOADED,
    isDeleted: Boolean = false,
    deletedAt: LocalDateTime? = null,
    hardDeleteAfter: LocalDateTime? = null,
) : BaseEntity() {
    @Column(nullable = false)
    var isDeleted: Boolean = isDeleted
        private set

    @Column(name = "deleted_at", nullable = true)
    var deletedAt: LocalDateTime? = deletedAt
        private set

    @Column(name = "hard_delete_after", nullable = true)
    var hardDeleteAfter: LocalDateTime? = hardDeleteAfter
        private set

    init {
        require(!isDeleted || deletedAt != null) { "삭제된 파일은 deletedAt이 필요합니다." }
        require(!isDeleted || hardDeleteAfter != null) { "삭제된 파일은 hardDeleteAfter가 필요합니다." }
    }

    fun markDeleted(now: LocalDateTime) {
        markDeleted(now, RETENTION_DAYS)
    }

    fun markDeletedForImmediateCleanup(now: LocalDateTime) {
        markDeleted(now, IMMEDIATE_CLEANUP_DAYS)
    }

    private fun markDeleted(
        now: LocalDateTime,
        retentionDays: Long,
    ) {
        if (isDeleted) return

        isDeleted = true
        deletedAt = now
        hardDeleteAfter = now.plusDays(retentionDays)
    }

    companion object {
        private const val RETENTION_DAYS = 30L
        private const val IMMEDIATE_CLEANUP_DAYS = 0L

        fun createUploaded(
            fileName: String,
            storageKey: String,
            fileSize: Long,
            contentType: String,
            ownerType: FileOwnerType,
            ownerId: Long,
        ): File {
            require(fileName.isNotBlank()) { "fileName은 비어 있을 수 없습니다." }
            require(fileSize > 0) { "fileSize는 0보다 커야 합니다." }
            require(ownerId > 0) { "ownerId는 0보다 커야 합니다." }

            return File(
                fileName = fileName,
                storageKey = StorageKey(storageKey),
                fileSize = fileSize,
                contentType = FileContentType(contentType),
                ownerType = ownerType,
                ownerId = ownerId,
                status = FileStatus.UPLOADED,
            )
        }
    }
}
