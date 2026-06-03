package com.weeth.domain.account.domain.entity

import com.weeth.domain.account.domain.enums.AccountPaymentStatus
import com.weeth.domain.account.domain.enums.AccountTargetStatus
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.account.fixture.AccountTestFixture
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class AccountPaymentTargetTest :
    StringSpec({
        "createTargeted는 납부 대상을 미납 상태로 생성한다" {
            val account = AccountTestFixture.createAccount()
            val clubMember = ClubMemberTestFixture.createActiveMember(club = account.club)

            val target =
                AccountPaymentTarget.createTargeted(
                    account = account,
                    clubMember = clubMember,
                    dueAmount = Money.of(50_000),
                )

            target.targetStatus shouldBe AccountTargetStatus.TARGETED
            target.paymentStatus shouldBe AccountPaymentStatus.UNPAID
            target.dueAmount shouldBe 50_000
            target.paidAmount shouldBe 0
        }

        "exclude는 제외 대상과 0원 미납 상태로 변경한다" {
            val account = AccountTestFixture.createAccount()
            val clubMember = ClubMemberTestFixture.createActiveMember(club = account.club)
            val target = AccountPaymentTarget.createTargeted(account, clubMember, Money.of(50_000))

            target.exclude()

            target.targetStatus shouldBe AccountTargetStatus.EXCLUDED
            target.paymentStatus shouldBe AccountPaymentStatus.UNPAID
            target.dueAmount shouldBe 0
            target.paidAmount shouldBe 0
        }

        "markPaid는 납부 완료 상태와 납부 정보를 기록한다" {
            val account = AccountTestFixture.createAccount()
            val clubMember = ClubMemberTestFixture.createActiveMember(club = account.club)
            val target = AccountPaymentTarget.createTargeted(account, clubMember, Money.of(50_000))
            val paidAt = LocalDateTime.of(2026, 3, 13, 10, 0)

            target.markPaid(Money.of(50_000), confirmedBy = 1L, paidAt = paidAt)

            target.paymentStatus shouldBe AccountPaymentStatus.PAID
            target.paidAmount shouldBe 50_000
            target.paidAt shouldBe paidAt
            target.confirmedBy shouldBe 1L
        }

        "이미 납부 완료된 대상은 다시 납부 완료 처리할 수 없다" {
            val account = AccountTestFixture.createAccount()
            val clubMember = ClubMemberTestFixture.createActiveMember(club = account.club)
            val target = AccountPaymentTarget.createTargeted(account, clubMember, Money.of(50_000))
            val paidAt = LocalDateTime.of(2026, 3, 13, 10, 0)
            target.markPaid(Money.of(50_000), confirmedBy = 1L, paidAt = paidAt)

            shouldThrow<IllegalStateException> {
                target.markPaid(Money.of(50_000), confirmedBy = 1L, paidAt = paidAt.plusDays(1))
            }
        }

        "납부 금액이 대상 금액과 다르면 납부 완료 처리할 수 없다" {
            val account = AccountTestFixture.createAccount()
            val clubMember = ClubMemberTestFixture.createActiveMember(club = account.club)
            val target = AccountPaymentTarget.createTargeted(account, clubMember, Money.of(50_000))

            shouldThrow<IllegalArgumentException> {
                target.markPaid(Money.of(40_000), confirmedBy = 1L, paidAt = LocalDateTime.now())
            }
        }

        "제외 대상은 납부 완료 처리할 수 없다" {
            val account = AccountTestFixture.createAccount()
            val clubMember = ClubMemberTestFixture.createActiveMember(club = account.club)
            val target = AccountPaymentTarget.createExcluded(account, clubMember, memo = null)

            shouldThrow<IllegalStateException> {
                target.markPaid(Money.of(50_000), confirmedBy = 1L, paidAt = LocalDateTime.now())
            }
        }

        "markUnpaid는 납부 완료된 대상만 미납 처리할 수 있다" {
            val account = AccountTestFixture.createAccount()
            val clubMember = ClubMemberTestFixture.createActiveMember(club = account.club)
            val target = AccountPaymentTarget.createTargeted(account, clubMember, Money.of(50_000))
            val paidAt = LocalDateTime.of(2026, 3, 13, 10, 0)
            target.markPaid(Money.of(50_000), confirmedBy = 1L, paidAt = paidAt)

            target.markUnpaid()

            target.paymentStatus shouldBe AccountPaymentStatus.UNPAID
            target.paidAmount shouldBe 0
            target.paidAt shouldBe null
            target.confirmedBy shouldBe null
        }

        "미납 대상은 다시 미납 처리할 수 없다" {
            val account = AccountTestFixture.createAccount()
            val clubMember = ClubMemberTestFixture.createActiveMember(club = account.club)
            val target = AccountPaymentTarget.createTargeted(account, clubMember, Money.of(50_000))

            shouldThrow<IllegalStateException> {
                target.markUnpaid()
            }
        }

        "제외 대상은 미납 처리할 수 없다" {
            val account = AccountTestFixture.createAccount()
            val clubMember = ClubMemberTestFixture.createActiveMember(club = account.club)
            val target = AccountPaymentTarget.createExcluded(account, clubMember, memo = null)

            shouldThrow<IllegalStateException> {
                target.markUnpaid()
            }
        }
    })
