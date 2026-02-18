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
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "FILE")
@Validated
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
        @RequestParam @NotEmpty fileNames: List<@NotBlank String>,
    ): CommonResponse<List<UrlResponse>> =
        CommonResponse.success(
            FileResponseCode.PRESIGNED_URL_GET_SUCCESS,
            generateFileUrlUsecase.generateFileUploadUrls(ownerType, fileNames),
        )
}
