package com.weeth.domain.account.application.dto.request

import com.weeth.domain.account.domain.enums.AccountTransactionType
import com.weeth.domain.file.application.dto.request.FileSaveRequest
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class SaveAccountTransactionRequest(
    @field:Schema(
        description = "거래 유형 (INCOME: 수입, EXPENSE: 지출). 시스템 거래(DUES/CARRY_OVER/REFUND)는 직접 등록 불가",
        example = "EXPENSE",
        allowableValues = ["INCOME", "EXPENSE"],
    )
    val type: AccountTransactionType,
    @field:Schema(description = "거래 금액 (원)", example = "50000")
    @field:Positive
    val amount: Int,
    @field:Schema(description = "거래 내용", example = "스터디 지원금")
    @field:NotBlank
    @field:Size(max = 30)
    val title: String,
    @field:Schema(description = "거래처", example = "인프런")
    @field:Size(max = 30)
    val source: String,
    @field:Schema(description = "거래 일자", example = "2026-07-20")
    val transactedAt: LocalDateTime,
    @field:Schema(description = "메모", nullable = true)
    @field:Size(max = 200)
    val memo: String? = null,
    @field:Schema(description = "영수증 파일 목록. 최대 1개", nullable = true)
    @field:Valid
    @field:Size(max = 1)
    val files: List<@NotNull FileSaveRequest>? = null,
)
