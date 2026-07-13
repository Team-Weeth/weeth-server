package com.weeth.domain.account.application.usecase.query

import com.weeth.domain.account.application.dto.request.AccountPaymentStatusFilter
import com.weeth.domain.account.application.exception.AccountNotActiveException
import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.application.mapper.AccountPaymentTargetMapper
import com.weeth.domain.account.domain.entity.AccountPaymentTarget
import com.weeth.domain.account.domain.enums.AccountPaymentStatus
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
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.Optional

class GetAccountPaymentTargetQueryServiceTest :
    DescribeSpec({
        val accountRepository = mockk<AccountRepository>()
        val paymentTargetRepository = mockk<AccountPaymentTargetRepository>()
        val clubMemberReader = mockk<ClubMemberReader>()
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>(relaxed = true)
        val fileAccessUrlPort = mockk<FileAccessUrlPort>()
        val accountPaymentTargetMapper = AccountPaymentTargetMapper(fileAccessUrlPort)
        val service =
            GetAccountPaymentTargetQueryService(
                accountRepository = accountRepository,
                paymentTargetRepository = paymentTargetRepository,
                clubMemberReader = clubMemberReader,
                clubPermissionPolicy = clubPermissionPolicy,
                accountPaymentTargetMapper = accountPaymentTargetMapper,
            )

        beforeTest {
            clearMocks(
                accountRepository,
                paymentTargetRepository,
                clubMemberReader,
                clubPermissionPolicy,
                fileAccessUrlPort,
            )
        }

        describe("findTargets") {
            it("전체 후보 멤버를 페이지로 조회하고 저장된 납부 대상 상태를 합친다") {
                val clubId = 1L
                val accountId = 10L
                val userId = 100L
                val club = ClubTestFixture.createClub(id = clubId)
                val account = AccountTestFixture.createAccount(id = accountId, club = club)
                val targetedMember = ClubMemberTestFixture.createActiveMember(id = 20L, club = club)
                val excludedMember = ClubMemberTestFixture.createActiveMember(id = 21L, club = club)
                val target = AccountPaymentTarget.createTargeted(account, targetedMember, Money.of(30_000))
                target.markPaid(Money.of(30_000), confirmedBy = userId, paidAt = LocalDateTime.of(2026, 3, 1, 10, 0))
                val pageable = PageRequest.of(0, 10)

                every { accountRepository.findById(accountId) } returns Optional.of(account)
                every { clubMemberReader.countActiveByClubIdAndCardinalNumber(clubId, 40) } returns 18L
                every {
                    paymentTargetRepository.countActiveClubMemberTargetsByAccountIdAndTargetStatus(
                        accountId,
                        AccountTargetStatus.TARGETED,
                    )
                } returns 12L
                every {
                    clubMemberReader.findActiveByClubIdAndCardinalNumberAndKeyword(
                        clubId = clubId,
                        cardinalNumber = 40,
                        keyword = "김",
                        pageable = pageable,
                    )
                } returns PageImpl(listOf(targetedMember, excludedMember), pageable, 18)
                every {
                    paymentTargetRepository.findAllByAccountIdAndClubMemberIdIn(
                        accountId,
                        listOf(20L, 21L),
                    )
                } returns
                    listOf(target)

                val result =
                    service.findTargets(
                        clubId = clubId,
                        accountId = accountId,
                        userId = userId,
                        page = 0,
                        size = 10,
                        keyword = "김",
                        targetStatus = null,
                    )

                result.summary.totalCount shouldBe 18
                result.summary.targetedCount shouldBe 12
                result.summary.excludedCount shouldBe 6
                result.targets.pageNumber shouldBe 0
                result.targets.pageSize shouldBe 10
                result.targets.totalElements shouldBe 18
                result.targets.content.size shouldBe 2
                result.targets.content[0]
                    .paymentTargetInfo.clubMemberId shouldBe 20L
                result.targets.content[0].targetStatus shouldBe AccountTargetStatus.TARGETED
                result.targets.content[0].paymentStatus shouldBe AccountPaymentStatus.PAID
                result.targets.content[0].paidAmount shouldBe 30_000
                result.targets.content[1]
                    .paymentTargetInfo.clubMemberId shouldBe 21L
                result.targets.content[1].targetStatus shouldBe AccountTargetStatus.EXCLUDED
                result.targets.content[1].paymentStatus shouldBe AccountPaymentStatus.UNPAID
            }

            it("선택됨 필터에서는 저장된 TARGETED 대상만 페이지로 조회한다") {
                val clubId = 1L
                val accountId = 10L
                val userId = 100L
                val club = ClubTestFixture.createClub(id = clubId)
                val account = AccountTestFixture.createAccount(id = accountId, club = club)
                val member = ClubMemberTestFixture.createActiveMember(id = 20L, club = club)
                val target = AccountPaymentTarget.createTargeted(account, member, Money.of(30_000))
                val pageable = PageRequest.of(0, 10)

                every { accountRepository.findById(accountId) } returns Optional.of(account)
                every { clubMemberReader.countActiveByClubIdAndCardinalNumber(clubId, 40) } returns 18L
                every {
                    paymentTargetRepository.countActiveClubMemberTargetsByAccountIdAndTargetStatus(
                        accountId,
                        AccountTargetStatus.TARGETED,
                    )
                } returns 12L
                every {
                    paymentTargetRepository.findAllActiveClubMemberTargetsByAccountIdAndTargetStatus(
                        accountId = accountId,
                        targetStatus = AccountTargetStatus.TARGETED,
                        keyword = null,
                        pageable = pageable,
                    )
                } returns PageImpl(listOf(target), pageable, 12)

                val result =
                    service.findTargets(
                        clubId = clubId,
                        accountId = accountId,
                        userId = userId,
                        page = 0,
                        size = 10,
                        keyword = null,
                        targetStatus = AccountTargetStatus.TARGETED,
                    )

                result.targets.totalElements shouldBe 12
                result.targets.content
                    .first()
                    .targetStatus shouldBe AccountTargetStatus.TARGETED
            }

            it("다른 동아리 장부이면 AccountNotFoundException을 던지고 대상 목록을 조회하지 않는다") {
                val clubId = 1L
                val accountId = 10L
                val userId = 100L
                val otherClub = ClubTestFixture.createClub(id = 2L, code = "OTHER-CLUB")
                val account = AccountTestFixture.createAccount(id = accountId, club = otherClub)

                every { accountRepository.findById(accountId) } returns Optional.of(account)

                shouldThrow<AccountNotFoundException> {
                    service.findTargets(
                        clubId = clubId,
                        accountId = accountId,
                        userId = userId,
                        page = 0,
                        size = 10,
                        keyword = null,
                        targetStatus = null,
                    )
                }

                verify(exactly = 0) { clubMemberReader.countActiveByClubIdAndCardinalNumber(any(), any()) }
                verify(exactly = 0) {
                    paymentTargetRepository.countActiveClubMemberTargetsByAccountIdAndTargetStatus(any(), any())
                }
            }
        }

        describe("findPaymentStatus") {
            val clubId = 1L
            val accountId = 10L
            val userId = 100L

            fun stubSummary() {
                every { paymentTargetRepository.sumPaidAmountByAccountId(accountId) } returns 30_000L
                every { paymentTargetRepository.sumDueAmountByAccountId(accountId) } returns 60_000L
                every {
                    paymentTargetRepository.countByAccountIdAndTargetStatus(accountId, AccountTargetStatus.TARGETED)
                } returns 2L
                every {
                    paymentTargetRepository.countByAccountIdAndTargetStatusAndPaymentStatus(
                        accountId,
                        AccountTargetStatus.TARGETED,
                        AccountPaymentStatus.PAID,
                    )
                } returns 1L
                every {
                    paymentTargetRepository.countByAccountIdAndTargetStatusAndPaymentStatus(
                        accountId,
                        AccountTargetStatus.TARGETED,
                        AccountPaymentStatus.UNPAID,
                    )
                } returns 1L
                every {
                    paymentTargetRepository.countByAccountIdAndTargetStatusAndPaymentStatus(
                        accountId,
                        AccountTargetStatus.TARGETED,
                        AccountPaymentStatus.REFUNDED,
                    )
                } returns 1L
                // 제외 카운트 = 활성 명부(5) − 활성 TARGETED(2) = 3
                every {
                    paymentTargetRepository.countActiveClubMemberTargetsByAccountIdAndTargetStatus(
                        accountId = accountId,
                        targetStatus = AccountTargetStatus.TARGETED,
                    )
                } returns 2L
                every { clubMemberReader.countActiveByClubIdAndCardinalNumber(clubId, 40) } returns 5L
            }

            it("DRAFT 장부면 AccountNotActiveException 을 던지고 목록을 조회하지 않는다") {
                val club = ClubTestFixture.createClub(id = clubId)
                val account =
                    AccountTestFixture.createAccount(id = accountId, club = club, status = AccountStatus.DRAFT)
                every { accountRepository.findById(accountId) } returns Optional.of(account)

                shouldThrow<AccountNotActiveException> {
                    service.findPaymentStatus(
                        clubId = clubId,
                        accountId = accountId,
                        userId = userId,
                        paymentStatusFilter = AccountPaymentStatusFilter.ALL,
                        keyword = null,
                        page = 0,
                        size = 20,
                    )
                }
                verify(exactly = 0) {
                    paymentTargetRepository.findActiveTargetsByPaymentStatusOrderByUnpaidFirst(
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                }
            }

            it("납부 대상 목록과 상단 요약(수납액/목표/납부율/카운트)을 함께 반환한다") {
                val club = ClubTestFixture.createClub(id = clubId)
                val account = AccountTestFixture.createAccount(id = accountId, club = club)
                val unpaidMember = ClubMemberTestFixture.createActiveMember(id = 20L, club = club)
                val paidMember = ClubMemberTestFixture.createActiveMember(id = 21L, club = club)
                val unpaidTarget = AccountPaymentTarget.createTargeted(account, unpaidMember, Money.of(30_000))
                val paidTarget =
                    AccountPaymentTarget.createTargeted(account, paidMember, Money.of(30_000)).apply {
                        markPaid(Money.of(30_000), confirmedBy = userId, paidAt = LocalDateTime.of(2026, 3, 1, 10, 0))
                    }
                val pageable = PageRequest.of(0, 20)

                every { accountRepository.findById(accountId) } returns Optional.of(account)
                stubSummary()
                // 미납 우선 정렬은 리포지토리가 담당하므로, 목록 순서는 그대로 통과시킨다.
                every {
                    paymentTargetRepository.findActiveTargetsByPaymentStatusOrderByUnpaidFirst(
                        accountId = accountId,
                        paymentStatus = null,
                        keyword = null,
                        pageable = pageable,
                    )
                } returns PageImpl(listOf(unpaidTarget, paidTarget), pageable, 2)

                val result =
                    service.findPaymentStatus(
                        clubId = clubId,
                        accountId = accountId,
                        userId = userId,
                        paymentStatusFilter = AccountPaymentStatusFilter.ALL,
                        keyword = null,
                        page = 0,
                        size = 20,
                    )

                result.summary.collectedAmount shouldBe 30_000
                result.summary.targetAmount shouldBe 60_000
                result.summary.paymentRate shouldBe 0.5
                result.summary.targetCount shouldBe 2
                result.summary.paidCount shouldBe 1
                result.summary.unpaidCount shouldBe 1
                result.summary.refundedCount shouldBe 1
                result.summary.excludedCount shouldBe 3
                result.summary.bankAccountPublic shouldBe false
                result.summary.bankAccount shouldBe null
                result.members.totalElements shouldBe 2
                result.members.content
                    .first()
                    .paymentStatus shouldBe AccountPaymentStatus.UNPAID
            }

            it("미납 필터는 UNPAID 도메인 상태로 변환해 조회한다") {
                val club = ClubTestFixture.createClub(id = clubId)
                val account = AccountTestFixture.createAccount(id = accountId, club = club)
                val pageable = PageRequest.of(0, 20)

                every { accountRepository.findById(accountId) } returns Optional.of(account)
                stubSummary()
                every {
                    paymentTargetRepository.findActiveTargetsByPaymentStatusOrderByUnpaidFirst(
                        accountId = accountId,
                        paymentStatus = AccountPaymentStatus.UNPAID,
                        keyword = null,
                        pageable = pageable,
                    )
                } returns PageImpl(emptyList(), pageable, 0)

                service.findPaymentStatus(
                    clubId = clubId,
                    accountId = accountId,
                    userId = userId,
                    paymentStatusFilter = AccountPaymentStatusFilter.UNPAID,
                    keyword = null,
                    page = 0,
                    size = 20,
                )

                verify(exactly = 1) {
                    paymentTargetRepository.findActiveTargetsByPaymentStatusOrderByUnpaidFirst(
                        accountId = accountId,
                        paymentStatus = AccountPaymentStatus.UNPAID,
                        keyword = null,
                        pageable = pageable,
                    )
                }
            }

            it("제외 필터는 납부 대상 쿼리 대신 명부 기반 제외 후보를 조회한다") {
                val club = ClubTestFixture.createClub(id = clubId)
                val account = AccountTestFixture.createAccount(id = accountId, club = club)
                val excludedMember = ClubMemberTestFixture.createActiveMember(id = 30L, club = club)
                val pageable = PageRequest.of(0, 20)

                every { accountRepository.findById(accountId) } returns Optional.of(account)
                stubSummary()
                every {
                    clubMemberReader.findExcludedPaymentTargetCandidatesByCardinal(
                        clubId = clubId,
                        cardinalNumber = 40,
                        accountId = accountId,
                        keyword = null,
                        pageable = pageable,
                    )
                } returns PageImpl(listOf(excludedMember), pageable, 1)
                // 제외 후보는 행이 없을 수 있으므로 저장된 대상과 합치는 단계에서 빈 결과를 반환한다.
                every {
                    paymentTargetRepository.findAllByAccountIdAndClubMemberIdIn(accountId, listOf(30L))
                } returns emptyList()

                val result =
                    service.findPaymentStatus(
                        clubId = clubId,
                        accountId = accountId,
                        userId = userId,
                        paymentStatusFilter = AccountPaymentStatusFilter.EXCLUDED,
                        keyword = null,
                        page = 0,
                        size = 20,
                    )

                result.members.totalElements shouldBe 1
                result.members.content
                    .first()
                    .targetStatus shouldBe AccountTargetStatus.EXCLUDED
                result.summary.excludedCount shouldBe 3
                verify(exactly = 0) {
                    paymentTargetRepository.findActiveTargetsByPaymentStatusOrderByUnpaidFirst(
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                }
            }
        }
    })
