package com.weeth.domain.account.application.dto.request

import com.weeth.domain.account.domain.enums.AccountTransactionType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class UpdateAccountTransactionRequest(
    @field:Schema(
        description = "거래 유형 (INCOME: 수입, EXPENSE: 지출). null=변경 안 함. 시스템 거래로는 변경 불가",
        example = "EXPENSE",
        allowableValues = ["INCOME", "EXPENSE"],
        nullable = true,
    )
    val type: AccountTransactionType? = null,
    @field:Schema(description = "거래 금액 (원). null=변경 안 함", example = "50000", nullable = true)
    @field:Positive
    val amount: Int? = null,
    @field:Schema(description = "거래 내용. null=변경 안 함", example = "스터디 지원금", nullable = true)
    @field:Size(max = 30)
    val title: String? = null,
    @field:Schema(description = "거래처. null=변경 안 함", example = "인프런", nullable = true)
    @field:Size(max = 30)
    val source: String? = null,
    @field:Schema(description = "거래 일자. null=변경 안 함", example = "2026-07-20", nullable = true)
    val transactedAt: LocalDate? = null,
    @field:Schema(description = "메모. null=변경 안 함", nullable = true)
    @field:Size(max = 200)
    val memo: String? = null,
)
