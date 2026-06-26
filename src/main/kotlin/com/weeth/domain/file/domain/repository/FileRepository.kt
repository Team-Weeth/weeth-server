package com.weeth.domain.file.domain.repository

import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.enums.FileStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface FileRepository :
    JpaRepository<File, Long>,
    FileReader {
    fun findAllByOwnerTypeAndOwnerIdAndIsDeletedFalse(
        ownerType: FileOwnerType,
        ownerId: Long,
    ): List<File>

    fun findAllByOwnerTypeAndOwnerIdAndStatusAndIsDeletedFalse(
        ownerType: FileOwnerType,
        ownerId: Long,
        status: FileStatus,
    ): List<File>

    fun findAllByOwnerTypeAndOwnerIdInAndIsDeletedFalse(
        ownerType: FileOwnerType,
        ownerIds: List<Long>,
    ): List<File>

    fun findAllByOwnerTypeAndOwnerIdInAndStatusAndIsDeletedFalse(
        ownerType: FileOwnerType,
        ownerIds: List<Long>,
        status: FileStatus,
    ): List<File>

    fun existsByOwnerTypeAndOwnerIdAndIsDeletedFalse(
        ownerType: FileOwnerType,
        ownerId: Long,
    ): Boolean

    fun existsByOwnerTypeAndOwnerIdAndStatusAndIsDeletedFalse(
        ownerType: FileOwnerType,
        ownerId: Long,
        status: FileStatus,
    ): Boolean

    @Modifying(flushAutomatically = true)
    @Query(
        """
        UPDATE File f
        SET f.isDeleted = true,
            f.deletedAt = :deletedAt,
            f.hardDeleteAfter = :hardDeleteAfter,
            f.modifiedAt = :deletedAt
        WHERE f.ownerType = :ownerType
          AND f.ownerId = :ownerId
          AND f.status = com.weeth.domain.file.domain.enums.FileStatus.UPLOADED
          AND f.isDeleted = false
        """,
    )
    fun markActiveDeletedByOwnerTypeAndOwnerId(
        @Param("ownerType") ownerType: FileOwnerType,
        @Param("ownerId") ownerId: Long,
        @Param("deletedAt") deletedAt: LocalDateTime,
        @Param("hardDeleteAfter") hardDeleteAfter: LocalDateTime,
    ): Int

    @Modifying(flushAutomatically = true)
    @Query(
        """
        UPDATE File f
        SET f.isDeleted = true,
            f.deletedAt = :deletedAt,
            f.hardDeleteAfter = :hardDeleteAfter,
            f.modifiedAt = :deletedAt
        WHERE f.ownerType = :ownerType
          AND f.ownerId IN :ownerIds
          AND f.status = com.weeth.domain.file.domain.enums.FileStatus.UPLOADED
          AND f.isDeleted = false
        """,
    )
    fun markActiveDeletedByOwnerTypeAndOwnerIdIn(
        @Param("ownerType") ownerType: FileOwnerType,
        @Param("ownerIds") ownerIds: List<Long>,
        @Param("deletedAt") deletedAt: LocalDateTime,
        @Param("hardDeleteAfter") hardDeleteAfter: LocalDateTime,
    ): Int

    fun findAllActiveByOwnerTypeAndOwnerId(
        ownerType: FileOwnerType,
        ownerId: Long,
    ): List<File> = findAllByOwnerTypeAndOwnerIdAndStatusAndIsDeletedFalse(ownerType, ownerId, FileStatus.UPLOADED)

    fun findAllActiveByOwnerTypeAndOwnerIdIn(
        ownerType: FileOwnerType,
        ownerIds: List<Long>,
    ): List<File> =
        if (ownerIds.isEmpty()) {
            emptyList()
        } else {
            findAllByOwnerTypeAndOwnerIdInAndStatusAndIsDeletedFalse(ownerType, ownerIds, FileStatus.UPLOADED)
        }

    override fun findAll(
        ownerType: FileOwnerType,
        ownerId: Long,
        status: FileStatus?,
    ): List<File> =
        status?.let { findAllByOwnerTypeAndOwnerIdAndStatusAndIsDeletedFalse(ownerType, ownerId, it) }
            ?: findAllByOwnerTypeAndOwnerIdAndIsDeletedFalse(ownerType, ownerId)

    override fun findAll(
        ownerType: FileOwnerType,
        ownerIds: List<Long>,
        status: FileStatus?,
    ): List<File> {
        if (ownerIds.isEmpty()) {
            return emptyList()
        }
        return status?.let { findAllByOwnerTypeAndOwnerIdInAndStatusAndIsDeletedFalse(ownerType, ownerIds, it) }
            ?: findAllByOwnerTypeAndOwnerIdInAndIsDeletedFalse(ownerType, ownerIds)
    }

    override fun exists(
        ownerType: FileOwnerType,
        ownerId: Long,
        status: FileStatus?,
    ): Boolean =
        status?.let { existsByOwnerTypeAndOwnerIdAndStatusAndIsDeletedFalse(ownerType, ownerId, it) }
            ?: existsByOwnerTypeAndOwnerIdAndIsDeletedFalse(ownerType, ownerId)
}
