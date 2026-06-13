package com.weeth.domain.account.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class BankAccountResponse(
    @field:Schema(description = "은행명", example = "국민은행")
    val bankName: String,
    @field:Schema(description = "계좌번호", example = "123-456-789012")
    val accountNumber: String,
    @field:Schema(description = "예금주", example = "가천대 검도부")
    val holder: String,
    @field:Schema(description = "입금 안내 메모", example = "이름_회비 형식으로 입금해 주세요.", nullable = true)
    val guide: String?,
)
