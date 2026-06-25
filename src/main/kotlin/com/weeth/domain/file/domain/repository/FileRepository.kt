package com.weeth.domain.file.domain.repository

import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.enums.FileStatus
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

    fun findAllByOwnerTypeAndOwnerIdAndIsDeletedFalse(
        ownerType: FileOwnerType,
        ownerId: Long,
    ): List<File>

    fun findAllByOwnerTypeAndOwnerIdAndStatusAndIsDeletedFalse(
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

    fun findAllByOwnerTypeAndOwnerIdInAndIsDeletedFalse(
        ownerType: FileOwnerType,
        ownerIds: List<Long>,
    ): List<File>

    fun findAllByOwnerTypeAndOwnerIdInAndStatusAndIsDeletedFalse(
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

    fun existsByOwnerTypeAndOwnerIdAndIsDeletedFalse(
        ownerType: FileOwnerType,
        ownerId: Long,
    ): Boolean

    fun existsByOwnerTypeAndOwnerIdAndStatusAndIsDeletedFalse(
        ownerType: FileOwnerType,
        ownerId: Long,
        status: FileStatus,
    ): Boolean

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
