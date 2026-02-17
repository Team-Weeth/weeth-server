package com.weeth.domain.file.application.usecase.query

import com.weeth.domain.file.application.dto.response.UrlResponse
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.port.FileUploadUrlPort
import org.springframework.stereotype.Service

@Service
class FileQueryService(
    private val fileUploadUrlPort: FileUploadUrlPort,
    private val fileMapper: FileMapper,
) {
    fun generateFileUploadUrls(
        ownerType: FileOwnerType,
        fileNames: List<String>,
    ): List<UrlResponse> =
        fileNames
            .map { fileUploadUrlPort.generateUploadUrl(ownerType, it) }
            .map { fileMapper.toUrlResponse(it.fileName, it.url, it.storageKey) }
}
