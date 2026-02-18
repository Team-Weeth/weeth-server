package com.weeth.domain.file.presentation

import com.weeth.domain.file.application.dto.response.UrlResponse
import com.weeth.domain.file.application.exception.FileErrorCode
import com.weeth.domain.file.application.usecase.command.GenerateFileUrlUsecase
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "FILE")
@RestController
@RequestMapping("/api/v4/files")
@ApiErrorCodeExample(FileErrorCode::class)
class FileController(
    private val generateFileUrlUsecase: GenerateFileUrlUsecase,
) {
    @GetMapping
    @Operation(summary = "파일 업로드를 위한 presigned url을 요청하는 API 입니다.")
    fun getUrl(
        @Parameter(description = "파일 소유 타입", example = "POST")
        @RequestParam ownerType: FileOwnerType,
        @RequestParam fileNames: List<String>,
    ): CommonResponse<List<UrlResponse>> {
        require(fileNames.isNotEmpty()) { "fileName은 비어 있을 수 없습니다." }
        return CommonResponse.success(
            FileResponseCode.PRESIGNED_URL_GET_SUCCESS,
            generateFileUrlUsecase.generateFileUploadUrls(ownerType, fileNames),
        )
    }
}
