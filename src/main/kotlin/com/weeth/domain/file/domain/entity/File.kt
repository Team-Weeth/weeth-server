package com.weeth.domain.file.domain.entity

import com.weeth.domain.file.application.exception.UnsupportedFileContentTypeException
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "file")
class File(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,
    @Column(nullable = false)
    var fileName: String,
    @Column(nullable = false, length = 500, unique = true)
    val storageKey: String,
    @Column(nullable = false)
    val fileSize: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val ownerType: FileOwnerType,
    @Column(nullable = false)
    val ownerId: Long,
    @Column(nullable = false, length = 100)
    val contentType: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: FileStatus = FileStatus.UPLOADED,
) : BaseEntity() {
    fun markDeleted() {
        status = FileStatus.DELETED
    }

    companion object {
        private val ALLOWED_CONTENT_TYPES =
            setOf(
                "image/jpeg",
                "image/png",
                "image/webp",
                "application/pdf",
            )

        fun createUploaded(
            fileName: String,
            storageKey: String,
            fileSize: Long,
            contentType: String,
            ownerType: FileOwnerType,
            ownerId: Long,
        ): File {
            require(fileName.isNotBlank()) { "fileName은 비어 있을 수 없습니다." }
            require(storageKey.isNotBlank()) { "storageKey는 비어 있을 수 없습니다." }
            require(fileSize > 0) { "fileSize는 0보다 커야 합니다." }
            if (contentType !in ALLOWED_CONTENT_TYPES) {
                throw UnsupportedFileContentTypeException()
            }
            require(ownerId > 0) { "ownerId는 0보다 커야 합니다." }

            return File(
                fileName = fileName,
                storageKey = storageKey,
                fileSize = fileSize,
                contentType = contentType,
                ownerType = ownerType,
                ownerId = ownerId,
                status = FileStatus.UPLOADED,
            )
        }
    }
}
