package com.weeth.domain.account.fixture

import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.entity.Receipt
import java.time.LocalDate

object ReceiptTestFixture {
    fun createReceipt(
        id: Long = 1L,
        description: String = "간식비",
        source: String = "편의점",
        amount: Int = 10_000,
        date: LocalDate = LocalDate.of(2024, 9, 1),
        account: Account = AccountTestFixture.createAccount(),
    ): Receipt =
        Receipt(
            id = id,
            description = description,
            source = source,
            amount = amount,
            date = date,
            account = account,
        )
}
