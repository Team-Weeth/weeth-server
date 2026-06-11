package com.weeth.domain.account.domain.entity

import com.weeth.domain.account.domain.enums.AccountRegistrationStep
import com.weeth.domain.account.domain.enums.AccountStatus
import com.weeth.domain.account.domain.enums.AccountTransactionType
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.account.fixture.AccountTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

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
            val account = Account.create(ClubTestFixture.createClub(), "2학기 회비", 200_000, 41)

            account.currentAmount shouldBe 200_000
            account.totalAmount shouldBe 200_000
            account.cardinal shouldBe 41
        }

        "createDraft는 DRAFT 상태의 회비 장부 초안을 생성한다" {
            val club = ClubTestFixture.createClub()

            val account = Account.createDraft(club = club, cardinal = 4)

            account.club shouldBe club
            account.cardinal shouldBe 4
            account.status shouldBe AccountStatus.DRAFT
            account.name shouldBe null
            account.duesAmount shouldBe 0
            account.currentBalance shouldBe 0
        }

        "activate는 필수 정보가 채워진 초안을 ACTIVE 상태로 변경한다" {
            val account = Account.createDraft(club = ClubTestFixture.createClub(), cardinal = 4)

            account.updateBasicInfo(name = "4기 회비", duesAmount = Money.of(50_000), description = "정기 회비")
            account.activate()

            account.status shouldBe AccountStatus.ACTIVE
        }

        "markModifiedBy는 마지막 수정자 ID를 기록한다" {
            val account = Account.createDraft(club = ClubTestFixture.createClub(), cardinal = 4)

            account.markModifiedBy(100L)

            account.lastModifiedBy shouldBe 100L
        }

        "activate는 회비 이름이 없으면 IllegalStateException을 던진다" {
            val account = Account.createDraft(club = ClubTestFixture.createClub(), cardinal = 4)

            shouldThrow<IllegalStateException> {
                account.activate()
            }
        }

        "applyTransaction은 수입 거래를 currentBalance에 더한다" {
            val account = Account.createDraft(club = ClubTestFixture.createClub(), cardinal = 4)
            val transaction =
                AccountTransaction.create(
                    account = account,
                    type = AccountTransactionType.INCOME,
                    title = "통장 이자",
                    source = "토스",
                    amount = Money.of(23),
                    transactedAt = LocalDateTime.of(2026, 3, 24, 10, 0),
                )

            account.applyTransaction(transaction)

            account.currentBalance shouldBe 23
            transaction.isApplied shouldBe true
        }

        "applyTransaction은 같은 거래를 중복 반영할 수 없다" {
            val account = Account.createDraft(club = ClubTestFixture.createClub(), cardinal = 4)
            val transaction =
                AccountTransaction.create(
                    account = account,
                    type = AccountTransactionType.INCOME,
                    title = "통장 이자",
                    source = "토스",
                    amount = Money.of(23),
                    transactedAt = LocalDateTime.of(2026, 3, 24, 10, 0),
                )
            account.applyTransaction(transaction)

            shouldThrow<IllegalStateException> {
                account.applyTransaction(transaction)
            }
            account.currentBalance shouldBe 23
            account.currentAmount shouldBe 23
            transaction.isApplied shouldBe true
        }

        "revertTransaction은 반영된 거래를 되돌리고 다시 되돌릴 수 없다" {
            val account = Account.createDraft(club = ClubTestFixture.createClub(), cardinal = 4)
            val transaction =
                AccountTransaction.create(
                    account = account,
                    type = AccountTransactionType.INCOME,
                    title = "통장 이자",
                    source = "토스",
                    amount = Money.of(23),
                    transactedAt = LocalDateTime.of(2026, 3, 24, 10, 0),
                )
            account.applyTransaction(transaction)

            account.revertTransaction(transaction)

            account.currentBalance shouldBe 0
            account.currentAmount shouldBe 0
            transaction.isApplied shouldBe false
            shouldThrow<IllegalStateException> {
                account.revertTransaction(transaction)
            }
        }

        "revertTransaction은 반영되지 않은 거래를 되돌릴 수 없다" {
            val account = Account.createDraft(club = ClubTestFixture.createClub(), cardinal = 4)
            val transaction =
                AccountTransaction.create(
                    account = account,
                    type = AccountTransactionType.INCOME,
                    title = "통장 이자",
                    source = "토스",
                    amount = Money.of(23),
                    transactedAt = LocalDateTime.of(2026, 3, 24, 10, 0),
                )

            shouldThrow<IllegalStateException> {
                account.revertTransaction(transaction)
            }
        }

        "advanceRegistrationStep은 DRAFT에서 다음 단계로만 진행된다" {
            val account = Account.createDraft(club = ClubTestFixture.createClub(), cardinal = 4)
            account.advanceRegistrationStep(
                AccountRegistrationStep.PAYMENT_TARGETS,
            )
            account.registrationStep shouldBe
                AccountRegistrationStep.PAYMENT_TARGETS

            // 이미 지난 단계로 되돌릴 수 없다
            account.advanceRegistrationStep(AccountRegistrationStep.BASIC)
            account.registrationStep shouldBe
                AccountRegistrationStep.PAYMENT_TARGETS
        }

        "advanceRegistrationStep은 ACTIVE 상태에서 아무 변경도 하지 않는다" {
            val account = Account.createDraft(club = ClubTestFixture.createClub(), cardinal = 4)
            account.updateBasicInfo("4기 회비", Money.of(50_000), "정기 회비")
            account.activate()

            account.advanceRegistrationStep(AccountRegistrationStep.CARRY_OVER)

            account.registrationStep shouldBe
                AccountRegistrationStep.PAYMENT_TARGETS
        }

        "updateBasicInfo 호출 시 registrationStep이 PAYMENT_TARGETS로 진행된다" {
            val account = Account.createDraft(club = ClubTestFixture.createClub(), cardinal = 4)

            account.updateBasicInfo("4기 회비", Money.of(50_000), "정기 회비")

            account.registrationStep shouldBe
                AccountRegistrationStep.PAYMENT_TARGETS
        }

        "applyTransaction은 잔액보다 큰 지출 거래면 IllegalStateException을 던진다" {
            val account = Account.createDraft(club = ClubTestFixture.createClub(), cardinal = 4)
            val transaction =
                AccountTransaction.create(
                    account = account,
                    type = AccountTransactionType.EXPENSE,
                    title = "굿즈",
                    source = "oo팩토리",
                    amount = Money.of(142_000),
                    transactedAt = LocalDateTime.of(2026, 3, 17, 10, 0),
                )

            shouldThrow<IllegalStateException> {
                account.applyTransaction(transaction)
            }
        }
    })
