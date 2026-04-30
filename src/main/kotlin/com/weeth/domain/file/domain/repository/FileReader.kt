package com.weeth.domain.file.domain.repository

import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.enums.FileStatus

interface FileReader {
    fun findAll(
        ownerType: FileOwnerType,
        ownerId: Long,
        status: FileStatus? = FileStatus.UPLOADED,
    ): List<File>

    fun findAll(
        ownerType: FileOwnerType,
        ownerIds: List<Long>,
        status: FileStatus? = FileStatus.UPLOADED,
    ): List<File>

    fun exists(
        ownerType: FileOwnerType,
        ownerId: Long,
        status: FileStatus? = FileStatus.UPLOADED,
    ): Boolean
}
