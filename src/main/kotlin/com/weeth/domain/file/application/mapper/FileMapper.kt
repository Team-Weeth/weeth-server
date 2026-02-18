package com.weeth.domain.file.application.mapper

import com.weeth.domain.file.application.dto.request.FileSaveRequest
import com.weeth.domain.file.application.dto.response.FileResponse
import com.weeth.domain.file.application.dto.response.UrlResponse
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import org.springframework.stereotype.Component

@Component
class FileMapper(
    private val fileAccessUrlPort: FileAccessUrlPort,
) {
    fun toFileList(
        requests: List<FileSaveRequest>?,
        ownerType: FileOwnerType,
        ownerId: Long,
    ): List<File> {
        if (requests.isNullOrEmpty()) {
            return emptyList()
        }

        return requests.map {
            File.createUploaded(
                fileName = it.fileName,
                storageKey = it.storageKey,
                fileSize = it.fileSize,
                contentType = it.contentType,
                ownerType = ownerType,
                ownerId = ownerId,
            )
        }
    }

    fun toFileResponse(file: File) =
        FileResponse(
            fileId = file.id,
            fileName = file.fileName,
            fileUrl = fileAccessUrlPort.resolve(file.storageKey.value),
            storageKey = file.storageKey.value,
            fileSize = file.fileSize,
            contentType = file.contentType.value,
            status = file.status,
        )

    fun toUrlResponse(
        fileName: String,
        putUrl: String,
        storageKey: String,
    ) = UrlResponse(fileName = fileName, putUrl = putUrl, storageKey = storageKey)
}
