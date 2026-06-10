package com.weeth.domain.account.application.dto.request

import com.weeth.domain.account.domain.vo.BankAccount
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SaveAccountBankAccountRequest(
    @field:Schema(description = "계좌 공개 여부", example = "true")
    val bankAccountVisible: Boolean,
    @field:Schema(description = "입금 계좌 정보. bankAccountVisible=true 일 때 필수", nullable = true)
    @field:Valid
    val bankAccount: BankAccountRequest?,
)

data class BankAccountRequest(
    @field:Schema(description = "은행명", example = "국민은행")
    @field:NotBlank
    @field:Size(max = 30)
    val bankName: String,
    @field:Schema(description = "계좌번호", example = "123-456-789012")
    @field:NotBlank
    @field:Size(max = 50)
    val accountNumber: String,
    @field:Schema(description = "예금주", example = "가천대 검도부")
    @field:NotBlank
    @field:Size(max = 30)
    val holder: String,
    @field:Schema(description = "입금 안내 메모", example = "이름_회비 형식으로 입금해 주세요.", nullable = true)
    @field:Size(max = 30)
    val guide: String? = null,
) {
    fun toBankAccount(): BankAccount =
        BankAccount.of(
            bankName = bankName,
            accountNumber = accountNumber,
            holder = holder,
            guide = guide,
        )
}
