package com.weeth.domain.account.application.usecase.command

import com.weeth.domain.account.application.dto.request.ExcludePaymentTargetsRequest
import com.weeth.domain.account.application.dto.request.MarkPaymentPaidRequest
import com.weeth.domain.account.application.dto.request.MarkPaymentUnpaidRequest
import com.weeth.domain.account.application.dto.request.RefundPaymentRequest
import com.weeth.domain.account.application.exception.AccountPaymentNotRefundableException
import com.weeth.domain.account.application.exception.AccountPaymentTargetNotFoundException
import com.weeth.domain.account.application.exception.AccountPaymentTargetPaidException
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.entity.AccountPaymentTarget
import com.weeth.domain.account.domain.entity.AccountTransaction
import com.weeth.domain.account.domain.enums.AccountPaymentStatus
import com.weeth.domain.account.domain.enums.AccountTargetStatus
import com.weeth.domain.account.domain.enums.AccountTransactionType
import com.weeth.domain.account.domain.repository.AccountPaymentTargetRepository
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.account.domain.repository.AccountTransactionRepository
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.account.fixture.AccountTestFixture
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.util.ReflectionTestUtils
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ManageAccountPaymentUseCaseTest :
    DescribeSpec({
        val accountRepository = mockk<AccountRepository>()
        val paymentTargetRepository = mockk<AccountPaymentTargetRepository>()
        val transactionRepository = mockk<AccountTransactionRepository>(relaxed = true)
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>(relaxed = true)
        val clock = Clock.fixed(Instant.parse("2026-06-20T00:00:00Z"), ZoneId.of("Asia/Seoul"))
        val useCase =
            ManageAccountPaymentUseCase(
                accountRepository,
                paymentTargetRepository,
                transactionRepository,
                clubPermissionPolicy,
                clock,
            )

        val userId = 10L
        val accountId = 1L
        val adminMember = ClubMemberTestFixture.createAdminMember(user = UserTestFixture.createAdmin(id = userId))

        beforeTest {
            clearMocks(accountRepository, paymentTargetRepository, transactionRepository, clubPermissionPolicy)
            every { clubPermissionPolicy.requireAdmin(any(), userId) } returns adminMember
        }

        fun target(
            account: Account,
            id: Long,
            due: Int,
            paid: Boolean,
        ): AccountPaymentTarget {
            val member = ClubMemberTestFixture.createActiveMember(club = account.club)
            val t = AccountPaymentTarget.createTargeted(account, member, Money.of(due))
            ReflectionTestUtils.setField(t, "id", id)
            if (paid) t.markPaid(Money.of(due), confirmedBy = 99L, paidAt = LocalDateTime.now())
            return t
        }

        describe("markPaid") {
            it("납부 대상에 DUES 수입 거래를 생성하고 잔액을 가산하며 상태를 PAID로 만든다") {
                val account = AccountTestFixture.createAccount(id = accountId, currentBalance = 100_000)
                val target = target(account, id = 100L, due = 30_000, paid = false)
                every { accountRepository.findByIdWithLock(accountId) } returns account
                every { paymentTargetRepository.findAllByAccountIdAndIdIn(accountId, listOf(100L)) } returns
                    listOf(target)

                useCase.markPaid(account.club.id, accountId, MarkPaymentPaidRequest(listOf(100L)), userId)

                target.paymentStatus shouldBe AccountPaymentStatus.PAID
                account.currentBalance shouldBe 130_000
                account.lastModifiedBy shouldBe userId
                verify(exactly = 1) {
                    transactionRepository.saveAll(
                        match<List<AccountTransaction>> {
                            it.size == 1 && it.first().type == AccountTransactionType.DUES &&
                                it.first().amount == 30_000 &&
                                it.first().registeredByName == "적순"
                        },
                    )
                }
            }

            it("존재하지 않는 대상이 포함되면 NotFound를 던진다") {
                val account = AccountTestFixture.createAccount(id = accountId)
                every { accountRepository.findByIdWithLock(accountId) } returns account
                every { paymentTargetRepository.findAllByAccountIdAndIdIn(accountId, listOf(100L, 200L)) } returns
                    listOf(target(account, id = 100L, due = 30_000, paid = false))

                shouldThrow<AccountPaymentTargetNotFoundException> {
                    useCase.markPaid(account.club.id, accountId, MarkPaymentPaidRequest(listOf(100L, 200L)), userId)
                }
            }
        }

        describe("markUnpaid") {
            it("대상의 활성 DUES 거래를 원복하고 미납 상태로 되돌린다") {
                val account = AccountTestFixture.createAccount(id = accountId, currentBalance = 100_000)
                val target = target(account, id = 100L, due = 30_000, paid = true)
                val dues =
                    AccountTransaction.create(
                        account = account,
                        type = AccountTransactionType.DUES,
                        title = "회비 납부",
                        source = null,
                        amount = Money.of(30_000),
                        transactedAt = LocalDateTime.now(),
                        paymentTarget = target,
                    )
                account.applyTransaction(dues)
                account.currentBalance shouldBe 130_000
                every { accountRepository.findByIdWithLock(accountId) } returns account
                every { paymentTargetRepository.findAllByAccountIdAndIdIn(accountId, listOf(100L)) } returns
                    listOf(target)
                every {
                    transactionRepository.findByPaymentTargetIdAndTypeAndDeletedAtIsNull(
                        100L,
                        AccountTransactionType.DUES,
                    )
                } returns dues

                useCase.markUnpaid(account.club.id, accountId, MarkPaymentUnpaidRequest(listOf(100L)), userId)

                target.paymentStatus shouldBe AccountPaymentStatus.UNPAID
                account.currentBalance shouldBe 100_000
                dues.deletedAt.shouldNotBeNull()
            }
        }

        describe("refund") {
            it("DUES는 보존하고 REFUND 지출을 생성해 잔액을 차감하며 상태를 REFUNDED로 만든다") {
                val account = AccountTestFixture.createAccount(id = accountId, currentBalance = 130_000)
                val target = target(account, id = 100L, due = 30_000, paid = true)
                every { accountRepository.findByIdWithLock(accountId) } returns account
                every { paymentTargetRepository.findAllByAccountIdAndIdIn(accountId, listOf(100L)) } returns
                    listOf(target)

                useCase.refund(account.club.id, accountId, RefundPaymentRequest(listOf(100L)), userId)

                target.paymentStatus shouldBe AccountPaymentStatus.REFUNDED
                target.refundedBy shouldBe userId
                account.currentBalance shouldBe 100_000
                verify(exactly = 1) {
                    transactionRepository.saveAll(
                        match<List<AccountTransaction>> {
                            it.size == 1 && it.first().type == AccountTransactionType.REFUND &&
                                it.first().amount == 30_000 &&
                                it.first().registeredByName == "적순"
                        },
                    )
                }
            }

            it("납부 완료 상태가 아닌 대상은 환불할 수 없다") {
                val account = AccountTestFixture.createAccount(id = accountId, currentBalance = 100_000)
                val target = target(account, id = 100L, due = 30_000, paid = false)
                every { accountRepository.findByIdWithLock(accountId) } returns account
                every { paymentTargetRepository.findAllByAccountIdAndIdIn(accountId, listOf(100L)) } returns
                    listOf(target)

                shouldThrow<AccountPaymentNotRefundableException> {
                    useCase.refund(account.club.id, accountId, RefundPaymentRequest(listOf(100L)), userId)
                }
            }
        }

        describe("exclude") {
            it("선택한 미납 대상들을 EXCLUDED로 전이하고 마지막 수정자를 기록한다") {
                val account = AccountTestFixture.createAccount(id = accountId, currentBalance = 100_000)
                val target1 = target(account, id = 100L, due = 30_000, paid = false)
                val target2 = target(account, id = 200L, due = 30_000, paid = false)
                every { accountRepository.findByIdWithLock(accountId) } returns account
                every { paymentTargetRepository.findAllByAccountIdAndIdIn(accountId, listOf(100L, 200L)) } returns
                    listOf(target1, target2)

                useCase.exclude(account.club.id, accountId, ExcludePaymentTargetsRequest(listOf(100L, 200L)), userId)

                target1.targetStatus shouldBe AccountTargetStatus.EXCLUDED
                target2.targetStatus shouldBe AccountTargetStatus.EXCLUDED
                target1.dueAmount shouldBe 0
                account.lastModifiedBy shouldBe userId
            }

            it("납부 완료(PAID) 대상이 포함되면 예외를 던지고 아무 대상도 변경하지 않는다") {
                val account = AccountTestFixture.createAccount(id = accountId, currentBalance = 100_000)
                val unpaid = target(account, id = 100L, due = 30_000, paid = false)
                val paid = target(account, id = 200L, due = 30_000, paid = true)
                every { accountRepository.findByIdWithLock(accountId) } returns account
                every { paymentTargetRepository.findAllByAccountIdAndIdIn(accountId, listOf(100L, 200L)) } returns
                    listOf(unpaid, paid)

                shouldThrow<AccountPaymentTargetPaidException> {
                    useCase.exclude(
                        account.club.id,
                        accountId,
                        ExcludePaymentTargetsRequest(listOf(100L, 200L)),
                        userId,
                    )
                }

                unpaid.targetStatus shouldBe AccountTargetStatus.TARGETED
                paid.targetStatus shouldBe AccountTargetStatus.TARGETED
            }

            it("존재하지 않는 대상이 포함되면 NotFound를 던진다") {
                val account = AccountTestFixture.createAccount(id = accountId)
                every { accountRepository.findByIdWithLock(accountId) } returns account
                every { paymentTargetRepository.findAllByAccountIdAndIdIn(accountId, listOf(100L, 200L)) } returns
                    listOf(target(account, id = 100L, due = 30_000, paid = false))

                shouldThrow<AccountPaymentTargetNotFoundException> {
                    useCase.exclude(
                        account.club.id,
                        accountId,
                        ExcludePaymentTargetsRequest(listOf(100L, 200L)),
                        userId,
                    )
                }
            }
        }
    })
