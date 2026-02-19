package com.weeth.domain.account.application.dto.response

import com.weeth.domain.file.application.dto.response.FileResponse
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class ReceiptResponse(
    @field:Schema(description = "영수증 ID", example = "1")
    val id: Long,
    @field:Schema(description = "영수증 설명", example = "간식비")
    val description: String?,
    @field:Schema(description = "출처", example = "편의점")
    val source: String?,
    @field:Schema(description = "사용 금액", example = "10000")
    val amount: Int,
    @field:Schema(description = "사용 날짜", example = "2024-09-01")
    val date: LocalDate,
    @field:Schema(description = "첨부 파일 목록")
    val fileUrls: List<FileResponse>,
)
