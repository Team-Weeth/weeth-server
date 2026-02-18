package com.weeth.domain.file.domain.repository

import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.entity.FileStatus
import org.springframework.data.jpa.repository.JpaRepository

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
