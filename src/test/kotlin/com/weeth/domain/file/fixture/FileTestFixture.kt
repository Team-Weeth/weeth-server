package com.weeth.domain.file.fixture

import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.vo.FileContentType
import com.weeth.domain.file.domain.vo.StorageKey

object FileTestFixture {
    fun createFile(
        id: Long,
        fileName: String,
        storageKey: StorageKey = StorageKey("NOTICE/2026-02/test.png"),
        fileSize: Long = 1024,
        ownerType: FileOwnerType = FileOwnerType.NOTICE,
        ownerId: Long = 1L,
        contentType: FileContentType = FileContentType("image/png"),
    ): File =
        File(
            id = id,
            fileName = fileName,
            storageKey = storageKey,
            fileSize = fileSize,
            ownerType = ownerType,
            ownerId = ownerId,
            contentType = contentType,
        )
}
