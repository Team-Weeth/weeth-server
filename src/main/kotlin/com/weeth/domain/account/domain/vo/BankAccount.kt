package com.weeth.domain.account.domain.vo

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

/**
 * 회비 장부에 노출되는 입금 계좌 정보를 저장하기 위한 VO.
 * 입력값은 모두 trim 후 저장되며, 비어 있을 수 없다.
 */
@Embeddable
class BankAccount(
    bankName: String,
    accountNumber: String,
    holder: String,
    guide: String? = null,
) {
    @Column(name = "bank_name", length = 30)
    var bankName: String = bankName
        private set

    @Column(name = "bank_account_number", length = 50)
    var accountNumber: String = accountNumber
        private set

    @Column(name = "account_holder", length = 50)
    var holder: String = holder
        private set

    @Column(name = "bank_guide", length = 200)
    var guide: String? = guide
        private set

    init {
        require(this.bankName.isNotBlank()) { "은행명은 비어 있을 수 없습니다." }
        require(this.accountNumber.isNotBlank()) { "계좌번호는 비어 있을 수 없습니다." }
        require(this.holder.isNotBlank()) { "예금주는 비어 있을 수 없습니다." }
    }

    companion object {
        fun of(
            bankName: String,
            accountNumber: String,
            holder: String,
            guide: String? = null,
        ): BankAccount =
            BankAccount(
                bankName = bankName.trim(),
                accountNumber = accountNumber.trim(),
                holder = holder.trim(),
                guide = guide?.trim()?.takeIf { it.isNotBlank() },
            )
    }
}
