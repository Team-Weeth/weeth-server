package com.weeth.domain.account.domain.entity

import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.account.fixture.AccountTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class AccountTest :
    StringSpec({
        "spend은 currentAmount를 Money 금액만큼 감소시킨다" {
            val account = AccountTestFixture.createAccount(currentAmount = 100_000)

            account.spend(Money.of(10_000))

            account.currentAmount shouldBe 90_000
        }

        "cancelSpend은 currentAmount를 Money 금액만큼 복원한다" {
            val account = AccountTestFixture.createAccount(currentAmount = 90_000)

            account.cancelSpend(Money.of(10_000))

            account.currentAmount shouldBe 100_000
        }

        "adjustSpend는 기존 금액을 취소하고 새 금액을 차감한다" {
            val account = AccountTestFixture.createAccount(totalAmount = 100_000, currentAmount = 90_000)

            account.adjustSpend(Money.of(10_000), Money.of(20_000))

            account.currentAmount shouldBe 80_000
        }

        "spend 시 잔액이 부족하면 IllegalStateException을 던진다" {
            val account = AccountTestFixture.createAccount(currentAmount = 5_000)

            shouldThrow<IllegalStateException> { account.spend(Money.of(10_000)) }
        }

        "cancelSpend 시 총액을 초과하면 IllegalStateException을 던진다" {
            val account = AccountTestFixture.createAccount(totalAmount = 100_000, currentAmount = 100_000)

            shouldThrow<IllegalStateException> { account.cancelSpend(Money.of(1)) }
        }

        "create는 currentAmount를 totalAmount와 동일하게 초기화한다" {
            val account = Account.create("2학기 회비", 200_000, 41)

            account.currentAmount shouldBe 200_000
            account.totalAmount shouldBe 200_000
            account.cardinal shouldBe 41
        }
    })
