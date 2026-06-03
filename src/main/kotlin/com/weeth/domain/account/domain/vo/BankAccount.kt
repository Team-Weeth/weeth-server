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
    var bankName: String = normalizeRequired(bankName, "은행명")
        private set

    @Column(name = "bank_account_number", length = 50)
    var accountNumber: String = normalizeRequired(accountNumber, "계좌번호")
        private set

    @Column(name = "account_holder", length = 50)
    var holder: String = normalizeRequired(holder, "예금주")
        private set

    @Column(name = "bank_guide", length = 200)
    var guide: String? = normalizeOptional(guide)
        private set

    companion object {
        fun of(
            bankName: String,
            accountNumber: String,
            holder: String,
            guide: String? = null,
        ): BankAccount =
            BankAccount(
                bankName = bankName,
                accountNumber = accountNumber,
                holder = holder,
                guide = guide,
            )

        private fun normalizeRequired(
            value: String,
            fieldName: String,
        ): String {
            val normalized = value.trim()
            require(normalized.isNotBlank()) { "$fieldName 은 비어 있을 수 없습니다." }
            return normalized
        }

        private fun normalizeOptional(value: String?): String? = value?.trim()?.takeIf { it.isNotBlank() }
    }
}
