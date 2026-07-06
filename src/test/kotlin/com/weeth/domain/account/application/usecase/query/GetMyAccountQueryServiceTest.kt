package com.weeth.domain.account.application.usecase.query

import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.application.mapper.MyAccountMapper
import com.weeth.domain.account.application.usecase.MemberAccountAccessResolver
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.entity.AccountPaymentTarget
import com.weeth.domain.account.domain.enums.AccountPaymentStatus
import com.weeth.domain.account.domain.enums.AccountStatus
import com.weeth.domain.account.domain.repository.AccountPaymentTargetRepository
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.account.domain.vo.BankAccount
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.repository.ClubMemberCardinalReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubMemberCardinalTestFixture
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class GetMyAccountQueryServiceTest :
    DescribeSpec({
        val accountRepository = mockk<AccountRepository>()
        val paymentTargetRepository = mockk<AccountPaymentTargetRepository>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val clubMemberCardinalReader = mockk<ClubMemberCardinalReader>()
        val queryService =
            GetMyAccountQueryService(
                accountRepository = accountRepository,
                paymentTargetRepository = paymentTargetRepository,
                clubMemberPolicy = clubMemberPolicy,
                clubMemberCardinalReader = clubMemberCardinalReader,
                memberAccountAccessResolver =
                    MemberAccountAccessResolver(accountRepository, clubMemberPolicy, clubMemberCardinalReader),
                myAccountMapper = MyAccountMapper(),
            )

        val club = ClubTestFixture.createClub(id = 1L)
        val userId = 10L
        val member =
            ClubMemberTestFixture.createActiveMember(
                id = 50L,
                club = club,
                user = UserTestFixture.createActiveUser1(id = userId),
            )

        fun account(
            id: Long,
            cardinal: Int,
            name: String = "${cardinal}기 회비",
            memberVisible: Boolean = true,
            bankAccountVisible: Boolean = true,
        ): Account =
            Account(
                id = id,
                club = club,
                cardinal = cardinal,
                name = name,
                duesAmount = 60_000,
                currentBalance = 152_129,
                bankAccount = BankAccount.of("국민은행", "12-12412-1231", "가천대 검도부", "이름_회비"),
                bankAccountVisible = bankAccountVisible,
                memberVisible = memberVisible,
                status = AccountStatus.ACTIVE,
            )

        fun targeted(
            account: Account,
            member: ClubMember,
            paid: Boolean = false,
        ): AccountPaymentTarget =
            AccountPaymentTarget
                .createTargeted(account = account, clubMember = member, dueAmount = Money.of(60_000))
                .also {
                    if (paid) {
                        it.markPaid(
                            amount = Money.of(60_000),
                            confirmedBy = 99L,
                            paidAt = LocalDateTime.of(2026, 7, 20, 12, 0),
                        )
                    }
                }

        fun participatedIn(vararg cardinalNumbers: Int) =
            cardinalNumbers.map {
                ClubMemberCardinalTestFixture.create(
                    clubMember = member,
                    cardinal = CardinalTestFixture.createCardinal(cardinalNumber = it),
                )
            }

        beforeTest {
            clearMocks(accountRepository, paymentTargetRepository, clubMemberPolicy, clubMemberCardinalReader)
            every { clubMemberPolicy.getActiveMember(club.id, userId) } returns member
            every { clubMemberCardinalReader.findAllByClubMember(member) } returns participatedIn(6, 7)
        }

        describe("findCardinals") {
            it("공개 ACTIVE 장부 기수만 최신순으로 반환하고 가장 높은 기수를 latest로 표시한다") {
                every {
                    accountRepository.findAllByClubIdAndStatusAndMemberVisibleTrueOrderByCardinalDesc(
                        club.id,
                        AccountStatus.ACTIVE,
                    )
                } returns listOf(account(id = 7L, cardinal = 7), account(id = 6L, cardinal = 6))

                val result = queryService.findCardinals(club.id, userId)

                result.map { it.cardinal } shouldBe listOf(7, 6)
                result[0].isLatest shouldBe true
                result[1].isLatest shouldBe false
                verify(exactly = 1) { clubMemberPolicy.getActiveMember(club.id, userId) }
            }

            it("내가 참여하지 않은 기수의 장부는 목록에서 제외한다") {
                every { clubMemberCardinalReader.findAllByClubMember(member) } returns participatedIn(7)
                every {
                    accountRepository.findAllByClubIdAndStatusAndMemberVisibleTrueOrderByCardinalDesc(
                        club.id,
                        AccountStatus.ACTIVE,
                    )
                } returns listOf(account(id = 7L, cardinal = 7), account(id = 6L, cardinal = 6))

                val result = queryService.findCardinals(club.id, userId)

                result.map { it.cardinal } shouldBe listOf(7)
                result[0].isLatest shouldBe true
            }

            it("참여한 기수가 없으면 빈 목록을 반환한다") {
                every { clubMemberCardinalReader.findAllByClubMember(member) } returns emptyList()
                every {
                    accountRepository.findAllByClubIdAndStatusAndMemberVisibleTrueOrderByCardinalDesc(
                        club.id,
                        AccountStatus.ACTIVE,
                    )
                } returns listOf(account(id = 7L, cardinal = 7), account(id = 6L, cardinal = 6))

                queryService.findCardinals(club.id, userId).shouldBeEmpty()
            }
        }

        describe("findMyAccount") {
            it("나의 납부 상태, 공개 계좌, 잔액과 목표액을 반환한다") {
                val account = account(id = 12L, cardinal = 7)
                val target = targeted(account, member)
                every {
                    accountRepository.findByClubIdAndCardinalAndStatusAndMemberVisibleTrue(
                        club.id,
                        7,
                        AccountStatus.ACTIVE,
                    )
                } returns account
                every { paymentTargetRepository.findByAccountIdAndClubMemberId(12L, 50L) } returns target
                every { paymentTargetRepository.sumDueAmountByAccountId(12L) } returns 1_425_000L

                val result = queryService.findMyAccount(club.id, cardinal = 7, userId = userId)

                result.accountId shouldBe 12L
                result.cardinal shouldBe 7
                result.duesAmount shouldBe 60_000
                result.myPayment.targeted shouldBe true
                result.myPayment.status shouldBe AccountPaymentStatus.UNPAID
                result.myPayment.dueAmount shouldBe 60_000
                result.bankAccountVisible shouldBe true
                result.bankAccount.shouldNotBeNull().accountNumber shouldBe "12-12412-1231"
                result.balance.currentBalance shouldBe 152_129
                result.balance.goalAmount shouldBe 1_425_000
            }

            it("계좌 비공개 장부는 bankAccountVisible=true가 아니면 계좌 정보를 숨긴다") {
                val account = account(id = 12L, cardinal = 7, bankAccountVisible = false)
                every {
                    accountRepository.findByClubIdAndCardinalAndStatusAndMemberVisibleTrue(
                        club.id,
                        7,
                        AccountStatus.ACTIVE,
                    )
                } returns account
                every { paymentTargetRepository.findByAccountIdAndClubMemberId(12L, 50L) } returns
                    targeted(account, member)
                every { paymentTargetRepository.sumDueAmountByAccountId(12L) } returns 1_425_000L

                val result = queryService.findMyAccount(club.id, cardinal = 7, userId = userId)

                result.bankAccountVisible shouldBe false
                result.bankAccount shouldBe null
            }

            it("납부 대상 행이 없으면 targeted=false로 반환한다") {
                val account = account(id = 12L, cardinal = 7)
                every {
                    accountRepository.findByClubIdAndCardinalAndStatusAndMemberVisibleTrue(
                        club.id,
                        7,
                        AccountStatus.ACTIVE,
                    )
                } returns account
                every { paymentTargetRepository.findByAccountIdAndClubMemberId(12L, 50L) } returns null
                every { paymentTargetRepository.sumDueAmountByAccountId(12L) } returns 1_425_000L

                val result = queryService.findMyAccount(club.id, cardinal = 7, userId = userId)

                result.myPayment.targeted shouldBe false
                result.myPayment.status shouldBe null
                result.myPayment.dueAmount shouldBe 0
                result.myPayment.paidAmount shouldBe 0
            }

            it("미공개 또는 없는 장부는 AccountNotFound로 은닉한다") {
                every {
                    accountRepository.findByClubIdAndCardinalAndStatusAndMemberVisibleTrue(
                        club.id,
                        7,
                        AccountStatus.ACTIVE,
                    )
                } returns null

                shouldThrow<AccountNotFoundException> {
                    queryService.findMyAccount(club.id, cardinal = 7, userId = userId)
                }
            }

            it("내가 참여하지 않은 기수의 장부는 AccountNotFound로 은닉한다") {
                every { clubMemberCardinalReader.findAllByClubMember(member) } returns participatedIn(6)
                every {
                    accountRepository.findByClubIdAndCardinalAndStatusAndMemberVisibleTrue(
                        club.id,
                        7,
                        AccountStatus.ACTIVE,
                    )
                } returns account(id = 12L, cardinal = 7)

                shouldThrow<AccountNotFoundException> {
                    queryService.findMyAccount(club.id, cardinal = 7, userId = userId)
                }
            }
        }
    })
