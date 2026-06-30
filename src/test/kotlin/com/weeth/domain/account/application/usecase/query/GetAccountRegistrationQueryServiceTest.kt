package com.weeth.domain.account.application.usecase.query

import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.application.mapper.AccountRegistrationMapper
import com.weeth.domain.account.domain.enums.AccountStatus
import com.weeth.domain.account.domain.enums.AccountTargetStatus
import com.weeth.domain.account.domain.repository.AccountPaymentTargetRepository
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.account.fixture.AccountTestFixture
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional

class GetAccountRegistrationQueryServiceTest :
    DescribeSpec({
        val accountRepository = mockk<AccountRepository>()
        val paymentTargetRepository = mockk<AccountPaymentTargetRepository>()
        val clubMemberReader = mockk<ClubMemberReader>()
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>(relaxed = true)
        val registrationMapper = AccountRegistrationMapper()
        val service =
            GetAccountRegistrationQueryService(
                accountRepository = accountRepository,
                paymentTargetRepository = paymentTargetRepository,
                clubMemberReader = clubMemberReader,
                clubPermissionPolicy = clubPermissionPolicy,
                registrationMapper = registrationMapper,
            )

        beforeTest {
            clearMocks(accountRepository, paymentTargetRepository, clubMemberReader, clubPermissionPolicy)
        }

        describe("findCarryOverSource") {
            it("직전 활성 기수 장부가 있으면 기수와 잔액을 반환한다") {
                val clubId = 1L
                val accountId = 10L
                val userId = 100L
                val club = ClubTestFixture.createClub(id = clubId)
                val account =
                    com.weeth.domain.account.domain.entity.Account
                        .createDraft(club = club, cardinal = 5)
                val previousAccount =
                    AccountTestFixture.createAccount(
                        id = 9L,
                        club = club,
                        cardinal = 3,
                        currentBalance = 240_000,
                    )

                every { accountRepository.findById(accountId) } returns Optional.of(account)
                every {
                    accountRepository.findTopByClubIdAndCardinalLessThanAndStatusOrderByCardinalDesc(
                        clubId = clubId,
                        cardinal = 5,
                        status = AccountStatus.ACTIVE,
                    )
                } returns previousAccount

                val result = service.findCarryOverSource(clubId = clubId, accountId = accountId, userId = userId)

                result.hasPreviousAccount shouldBe true
                result.cardinalNumber shouldBe 3
                result.balance shouldBe 240_000
            }

            it("직전 활성 기수 장부가 없으면 hasPreviousAccount=false를 반환한다") {
                val clubId = 1L
                val accountId = 10L
                val userId = 100L
                val club = ClubTestFixture.createClub(id = clubId)
                val account =
                    com.weeth.domain.account.domain.entity.Account
                        .createDraft(club = club, cardinal = 5)

                every { accountRepository.findById(accountId) } returns Optional.of(account)
                every {
                    accountRepository.findTopByClubIdAndCardinalLessThanAndStatusOrderByCardinalDesc(
                        clubId = clubId,
                        cardinal = 5,
                        status = AccountStatus.ACTIVE,
                    )
                } returns null

                val result = service.findCarryOverSource(clubId = clubId, accountId = accountId, userId = userId)

                result.hasPreviousAccount shouldBe false
                result.cardinalNumber shouldBe null
                result.balance shouldBe null
            }

            it("다른 동아리 장부이면 AccountNotFoundException을 던지고 이전 장부를 조회하지 않는다") {
                val clubId = 1L
                val accountId = 10L
                val userId = 100L
                val otherClub = ClubTestFixture.createClub(id = 2L, code = "OTHER-CLUB")
                val account =
                    com.weeth.domain.account.domain.entity.Account
                        .createDraft(club = otherClub, cardinal = 5)

                every { accountRepository.findById(accountId) } returns Optional.of(account)

                shouldThrow<AccountNotFoundException> {
                    service.findCarryOverSource(clubId = clubId, accountId = accountId, userId = userId)
                }

                verify(exactly = 0) {
                    accountRepository.findTopByClubIdAndCardinalLessThanAndStatusOrderByCardinalDesc(
                        any(),
                        any(),
                        any(),
                    )
                }
            }
        }

        describe("findStatus") {
            it("납부 대상/제외 대상 카운트와 이전 기수 잔액을 포함한다") {
                val clubId = 1L
                val accountId = 10L
                val userId = 100L
                val club = ClubTestFixture.createClub(id = clubId)
                val account =
                    com.weeth.domain.account.domain.entity.Account
                        .createDraft(club = club, cardinal = 5)
                account.updateBasicInfo(name = "5기 회비", duesAmount = Money.of(30_000), description = null)
                account.advanceRegistrationStep(
                    com.weeth.domain.account.domain.enums.AccountRegistrationStep.CARRY_OVER,
                )
                val previousAccount =
                    AccountTestFixture.createAccount(
                        id = 9L,
                        club = club,
                        cardinal = 3,
                        currentBalance = 240_000,
                    )

                every { accountRepository.findById(accountId) } returns Optional.of(account)
                every {
                    paymentTargetRepository.countActiveClubMemberTargetsByAccountIdAndTargetStatus(
                        accountId = accountId,
                        targetStatus = AccountTargetStatus.TARGETED,
                    )
                } returns 12L
                every { clubMemberReader.countActiveByClubIdAndCardinalNumber(clubId, 5) } returns 18L
                every {
                    accountRepository.findTopByClubIdAndCardinalLessThanAndStatusOrderByCardinalDesc(
                        clubId = clubId,
                        cardinal = 5,
                        status = AccountStatus.ACTIVE,
                    )
                } returns previousAccount

                val result = service.findStatus(clubId = clubId, accountId = accountId, userId = userId)

                result.paymentTargets?.targetCount shouldBe 12
                result.paymentTargets?.excludedCount shouldBe 6
                result.previousAccountBalance?.cardinalNumber shouldBe 3
                result.previousAccountBalance?.balance shouldBe 240_000
            }

            it("이월하기 0원을 저장한 경우 enabled=true로 복원한다") {
                val clubId = 1L
                val accountId = 10L
                val userId = 100L
                val club = ClubTestFixture.createClub(id = clubId)
                val account =
                    com.weeth.domain.account.domain.entity.Account
                        .createDraft(club = club, cardinal = 5)
                account.updateBasicInfo(name = "5기 회비", duesAmount = Money.of(30_000), description = null)
                account.updateCarryOver(enabled = true, amount = Money.ZERO, memo = "남은 금액 없음")

                every { accountRepository.findById(accountId) } returns Optional.of(account)
                every {
                    paymentTargetRepository.countActiveClubMemberTargetsByAccountIdAndTargetStatus(
                        accountId = accountId,
                        targetStatus = AccountTargetStatus.TARGETED,
                    )
                } returns 0L
                every { clubMemberReader.countActiveByClubIdAndCardinalNumber(clubId, 5) } returns 0L
                every {
                    accountRepository.findTopByClubIdAndCardinalLessThanAndStatusOrderByCardinalDesc(
                        clubId = clubId,
                        cardinal = 5,
                        status = AccountStatus.ACTIVE,
                    )
                } returns null

                val result = service.findStatus(clubId = clubId, accountId = accountId, userId = userId)

                result.carryOver?.enabled shouldBe true
                result.carryOver?.amount shouldBe 0
            }

            it("다른 동아리 장부이면 AccountNotFoundException을 던지고 납부 대상 집계를 조회하지 않는다") {
                val clubId = 1L
                val accountId = 10L
                val userId = 100L
                val otherClub = ClubTestFixture.createClub(id = 2L, code = "OTHER-CLUB")
                val account =
                    com.weeth.domain.account.domain.entity.Account
                        .createDraft(club = otherClub, cardinal = 5)

                every { accountRepository.findById(accountId) } returns Optional.of(account)

                shouldThrow<AccountNotFoundException> {
                    service.findStatus(clubId = clubId, accountId = accountId, userId = userId)
                }

                verify(exactly = 0) {
                    paymentTargetRepository.countActiveClubMemberTargetsByAccountIdAndTargetStatus(any(), any())
                }
                verify(exactly = 0) { clubMemberReader.countActiveByClubIdAndCardinalNumber(any(), any()) }
            }
        }
    })
