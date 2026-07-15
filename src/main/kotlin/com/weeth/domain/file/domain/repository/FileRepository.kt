package com.weeth.domain.file.domain.repository

import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.enums.FileStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FileRepository :
    JpaRepository<File, Long>,
    FileReader {
    fun findAllByOwnerTypeAndOwnerId(
        ownerType: FileOwnerType,
        ownerId: Long,
    ): List<File>

    fun findAllByOwnerTypeAndOwnerIdAndStatus(
        ownerType: FileOwnerType,
        ownerId: Long,
        status: FileStatus,
    ): List<File>

    fun findAllByOwnerTypeAndOwnerIdIn(
        ownerType: FileOwnerType,
        ownerIds: List<Long>,
    ): List<File>

    fun findAllByOwnerTypeAndOwnerIdInAndStatus(
        ownerType: FileOwnerType,
        ownerIds: List<Long>,
        status: FileStatus,
    ): List<File>

    fun existsByOwnerTypeAndOwnerId(
        ownerType: FileOwnerType,
        ownerId: Long,
    ): Boolean

    fun existsByOwnerTypeAndOwnerIdAndStatus(
        ownerType: FileOwnerType,
        ownerId: Long,
        status: FileStatus,
    ): Boolean

    @Modifying(flushAutomatically = true)
    @Query(
        """
        DELETE FROM File f
        WHERE f.ownerType = :ownerType
          AND f.ownerId = :ownerId
          AND f.status = com.weeth.domain.file.domain.enums.FileStatus.UPLOADED
        """,
    )
    fun hardDeleteActiveByOwnerTypeAndOwnerId(
        @Param("ownerType") ownerType: FileOwnerType,
        @Param("ownerId") ownerId: Long,
    ): Int

    fun hardDeleteActiveByOwnerTypeAndOwnerIdIn(
        ownerType: FileOwnerType,
        ownerIds: List<Long>,
    ): Int {
        if (ownerIds.isEmpty()) return 0
        return hardDeleteActiveByOwnerTypeAndOwnerIdInInternal(ownerType, ownerIds)
    }

    @Modifying(flushAutomatically = true)
    @Query(
        """
        DELETE FROM File f
        WHERE f.ownerType = :ownerType
          AND f.ownerId IN :ownerIds
          AND f.status = com.weeth.domain.file.domain.enums.FileStatus.UPLOADED
        """,
    )
    fun hardDeleteActiveByOwnerTypeAndOwnerIdInInternal(
        @Param("ownerType") ownerType: FileOwnerType,
        @Param("ownerIds") ownerIds: List<Long>,
    ): Int

    override fun findAll(
        ownerType: FileOwnerType,
        ownerId: Long,
        status: FileStatus?,
    ): List<File> =
        status?.let { findAllByOwnerTypeAndOwnerIdAndStatus(ownerType, ownerId, it) }
            ?: findAllByOwnerTypeAndOwnerId(ownerType, ownerId)

    override fun findAll(
        ownerType: FileOwnerType,
        ownerIds: List<Long>,
        status: FileStatus?,
    ): List<File> {
        if (ownerIds.isEmpty()) {
            return emptyList()
        }
        return status?.let { findAllByOwnerTypeAndOwnerIdInAndStatus(ownerType, ownerIds, it) }
            ?: findAllByOwnerTypeAndOwnerIdIn(ownerType, ownerIds)
    }

    override fun exists(
        ownerType: FileOwnerType,
        ownerId: Long,
        status: FileStatus?,
    ): Boolean =
        status?.let { existsByOwnerTypeAndOwnerIdAndStatus(ownerType, ownerId, it) }
            ?: existsByOwnerTypeAndOwnerId(ownerType, ownerId)
}
