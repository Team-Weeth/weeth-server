package com.weeth.domain.file.domain.entity

import com.weeth.domain.file.domain.vo.FileContentType
import com.weeth.domain.file.domain.vo.StorageKey
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.*

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
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: FileStatus = FileStatus.UPLOADED,
) : BaseEntity() {
    fun markDeleted() {
        status = FileStatus.DELETED
    }

    companion object {
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
