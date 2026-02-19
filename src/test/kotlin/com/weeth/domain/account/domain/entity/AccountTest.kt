package com.weeth.domain.account.domain.entity

import com.weeth.domain.account.fixture.AccountTestFixture
import com.weeth.domain.account.fixture.ReceiptTestFixture
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AccountTest :
    StringSpec({
        "spend은 currentAmount를 영수증 금액만큼 감소시킨다" {
            val account = AccountTestFixture.createAccount(currentAmount = 100_000)
            val receipt = ReceiptTestFixture.createReceipt(amount = 10_000, account = account)

            account.spend(receipt)

            account.currentAmount shouldBe 90_000
        }

        "cancel은 currentAmount를 영수증 금액만큼 복원한다" {
            val account = AccountTestFixture.createAccount(currentAmount = 100_000)
            val receipt = ReceiptTestFixture.createReceipt(amount = 10_000, account = account)
            account.spend(receipt)

            account.cancel(receipt)

            account.currentAmount shouldBe 100_000
        }
    })
