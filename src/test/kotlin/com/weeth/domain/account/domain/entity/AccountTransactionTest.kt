package com.weeth.domain.account.domain.entity

import com.weeth.domain.account.domain.enums.AccountTransactionDirection
import com.weeth.domain.account.domain.enums.AccountTransactionType
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.account.fixture.AccountTestFixture
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class AccountTransactionTest :
    StringSpec({
        "create는 거래 타입에 맞는 방향과 정규화된 문자열을 저장한다" {
            val account = AccountTestFixture.createAccount()

            val transaction =
                AccountTransaction.create(
                    account = account,
                    type = AccountTransactionType.EXPENSE,
                    title = " 굿즈(키링) ",
                    source = " oo팩토리 ",
                    amount = Money.of(142_000),
                    transactedAt = LocalDateTime.of(2026, 3, 17, 10, 0),
                    category = " 굿즈 ",
                    memo = " 영수증 확인 ",
                )

            transaction.direction shouldBe AccountTransactionDirection.EXPENSE
            transaction.title shouldBe "굿즈(키링)"
            transaction.source shouldBe "oo팩토리"
            transaction.category shouldBe "굿즈"
            transaction.memo shouldBe "영수증 확인"
            transaction.amount shouldBe 142_000
        }

        "금액은 0보다 커야 한다" {
            val account = AccountTestFixture.createAccount()

            shouldThrow<IllegalArgumentException> {
                AccountTransaction.create(
                    account = account,
                    type = AccountTransactionType.INCOME,
                    title = "통장 이자",
                    source = "토스",
                    amount = Money.ZERO,
                    transactedAt = LocalDateTime.of(2026, 3, 24, 10, 0),
                )
            }
        }

        "문자열 필드가 컬럼 길이를 초과하면 생성할 수 없다" {
            val account = AccountTestFixture.createAccount()

            shouldThrow<IllegalArgumentException> {
                AccountTransaction.create(
                    account = account,
                    type = AccountTransactionType.INCOME,
                    title = "가".repeat(101),
                    source = "토스",
                    amount = Money.of(1_000),
                    transactedAt = LocalDateTime.of(2026, 3, 24, 10, 0),
                )
            }

            shouldThrow<IllegalArgumentException> {
                AccountTransaction.create(
                    account = account,
                    type = AccountTransactionType.INCOME,
                    title = "통장 이자",
                    source = "가".repeat(51),
                    amount = Money.of(1_000),
                    transactedAt = LocalDateTime.of(2026, 3, 24, 10, 0),
                )
            }

            shouldThrow<IllegalArgumentException> {
                AccountTransaction.create(
                    account = account,
                    type = AccountTransactionType.INCOME,
                    title = "통장 이자",
                    source = "토스",
                    amount = Money.of(1_000),
                    transactedAt = LocalDateTime.of(2026, 3, 24, 10, 0),
                    category = "가".repeat(31),
                )
            }

            shouldThrow<IllegalArgumentException> {
                AccountTransaction.create(
                    account = account,
                    type = AccountTransactionType.INCOME,
                    title = "통장 이자",
                    source = "토스",
                    amount = Money.of(1_000),
                    transactedAt = LocalDateTime.of(2026, 3, 24, 10, 0),
                    memo = "가".repeat(201),
                )
            }
        }

        "다른 장부의 납부 대상은 거래에 연결할 수 없다" {
            val targetAccount = AccountTestFixture.createAccount(id = 1L)
            val transactionAccount = AccountTestFixture.createAccount(id = 2L)
            val clubMember = ClubMemberTestFixture.createActiveMember(club = targetAccount.club)
            val paymentTarget = AccountPaymentTarget.createTargeted(targetAccount, clubMember, Money.of(50_000))

            shouldThrow<IllegalStateException> {
                AccountTransaction.create(
                    account = transactionAccount,
                    type = AccountTransactionType.DUES,
                    title = "5기 회비",
                    source = null,
                    amount = Money.of(50_000),
                    transactedAt = LocalDateTime.of(2026, 3, 13, 10, 0),
                    paymentTarget = paymentTarget,
                )
            }
        }

        "softDelete는 삭제 시각을 기록한다" {
            val account = AccountTestFixture.createAccount()
            val transaction =
                AccountTransaction.create(
                    account = account,
                    type = AccountTransactionType.INCOME,
                    title = "통장 이자",
                    source = "토스",
                    amount = Money.of(23),
                    transactedAt = LocalDateTime.of(2026, 3, 24, 10, 0),
                )

            transaction.softDelete()

            transaction.deletedAt.shouldNotBeNull()
        }
    })
