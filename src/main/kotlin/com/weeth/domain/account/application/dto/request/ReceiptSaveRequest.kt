package com.weeth.domain.account.application.dto.request

import com.weeth.domain.file.application.dto.request.FileSaveRequest
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.LocalDate

data class ReceiptSaveRequest(
    @field:Schema(description = "영수증 설명", example = "간식비")
    val description: String?,
    @field:Schema(description = "출처", example = "편의점")
    val source: String?,
    @field:Schema(description = "사용 금액", example = "10000")
    @field:NotNull
    @field:Positive
    val amount: Int,
    @field:Schema(description = "사용 날짜", example = "2024-09-01")
    @field:NotNull
    val date: LocalDate,
    @field:Schema(description = "기수", example = "4")
    @field:NotNull
    val cardinal: Int,
    @field:Valid
    val files: List<@NotNull FileSaveRequest>?,
)
