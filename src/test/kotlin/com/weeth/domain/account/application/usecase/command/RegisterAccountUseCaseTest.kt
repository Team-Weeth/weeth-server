package com.weeth.domain.account.application.usecase.command

import com.weeth.domain.account.application.dto.request.BankAccountRequest
import com.weeth.domain.account.application.dto.request.SaveAccountBankAccountRequest
import com.weeth.domain.account.application.dto.request.SaveAccountBasicRequest
import com.weeth.domain.account.application.dto.request.SaveAccountCarryOverRequest
import com.weeth.domain.account.application.dto.request.SavePaymentTargetsRequest
import com.weeth.domain.account.application.exception.AccountCarryOverAmountMismatchException
import com.weeth.domain.account.application.exception.AccountExistsException
import com.weeth.domain.account.application.exception.AccountInvalidDraftStateException
import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.application.exception.AccountPaymentTargetMemberInvalidException
import com.weeth.domain.account.application.exception.AccountPaymentTargetPaidException
import com.weeth.domain.account.application.exception.AccountRegistrationStepIncompleteException
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.entity.AccountPaymentTarget
import com.weeth.domain.account.domain.enums.AccountPaymentStatus
import com.weeth.domain.account.domain.enums.AccountRegistrationStep
import com.weeth.domain.account.domain.enums.AccountStatus
import com.weeth.domain.account.domain.enums.AccountTargetStatus
import com.weeth.domain.account.domain.enums.AccountTransactionType
import com.weeth.domain.account.domain.repository.AccountPaymentTargetRepository
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.account.domain.repository.AccountTransactionRepository
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.account.fixture.AccountTestFixture
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberCardinalReader
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubMemberCardinalTestFixture
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDateTime

class RegisterAccountUseCaseTest :
    DescribeSpec({
        val accountRepository = mockk<AccountRepository>(relaxed = true)
        val paymentTargetRepository = mockk<AccountPaymentTargetRepository>(relaxed = true)
        val transactionRepository = mockk<AccountTransactionRepository>(relaxed = true)
        val cardinalReader = mockk<CardinalReader>(relaxed = true)
        val clubReader = mockk<ClubReader>(relaxed = true)
        val clubMemberCardinalReader = mockk<ClubMemberCardinalReader>()
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>(relaxed = true)
        val userReader = mockk<UserReader>()
        val useCase =
            RegisterAccountUseCase(
                accountRepository = accountRepository,
                paymentTargetRepository = paymentTargetRepository,
                transactionRepository = transactionRepository,
                cardinalReader = cardinalReader,
                clubReader = clubReader,
                clubMemberCardinalReader = clubMemberCardinalReader,
                clubPermissionPolicy = clubPermissionPolicy,
                userReader = userReader,
            )

        val clubId = 1L
        val accountId = 10L
        val userId = 100L
        val club = ClubTestFixture.createClub(id = clubId)

        beforeTest {
            clearMocks(
                accountRepository,
                paymentTargetRepository,
                transactionRepository,
                cardinalReader,
                clubReader,
                clubMemberCardinalReader,
                clubPermissionPolicy,
                userReader,
            )
            every { clubReader.getClubById(clubId) } returns club
        }

        describe("createDraft") {
            it("같은 기수에 작성 중인 초안이 있으면 기존 초안 정보와 마지막 수정자 이름을 반환한다") {
                val draft = Account.createDraft(club = club, cardinal = 5)
                draft.markModifiedBy(200L)
                every { userReader.findByIdOrNull(200L) } returns UserTestFixture.createActiveUser1(id = 200L)
                every { accountRepository.findByClubIdAndCardinal(clubId, 5) } returns draft

                val result = useCase.createDraft(clubId, cardinal = 5, userId = userId)

                result.accountId shouldBe draft.id
                result.isNew shouldBe false
                result.lastModifiedByName shouldBe "적순"
                verify(exactly = 0) { accountRepository.save(any()) }
            }

            it("같은 기수에 활성 장부가 있으면 AccountExistsException을 던진다") {
                val active = Account.createDraft(club = club, cardinal = 5)
                active.updateBasicInfo("5기 회비", Money.of(50_000), "정기 회비")
                active.activate()
                every { accountRepository.findByClubIdAndCardinal(clubId, 5) } returns active

                shouldThrow<AccountExistsException> { useCase.createDraft(clubId, cardinal = 5, userId = userId) }
            }

            it("기존 장부가 없으면 DRAFT 장부를 저장한다") {
                every { accountRepository.findByClubIdAndCardinal(clubId, 5) } returns null
                every { cardinalReader.findByClubIdAndCardinalNumber(clubId, 5) } returns
                    CardinalTestFixture.createCardinal(cardinalNumber = 5)
                every { accountRepository.save(any()) } answers { firstArg() }

                val result = useCase.createDraft(clubId, cardinal = 5, userId = userId)

                result.isNew shouldBe true
                result.lastModifiedByName shouldBe null
                verify(exactly = 1) {
                    accountRepository.save(
                        match {
                            it.status == AccountStatus.DRAFT &&
                                it.cardinal == 5 &&
                                it.lastModifiedBy == userId
                        },
                    )
                }
            }
        }

        describe("discardDraft") {
            it("초안 상태의 장부를 납부 대상 행과 함께 삭제한다") {
                val draft = Account.createDraft(club = club, cardinal = 5)
                every { accountRepository.findByIdWithLock(1L) } returns draft

                useCase.discardDraft(clubId = clubId, accountId = 1L, userId = userId)

                verify(exactly = 1) { paymentTargetRepository.deleteAllByAccountId(draft.id) }
                verify(exactly = 1) { accountRepository.delete(draft) }
            }

            it("활성 장부는 삭제하지 않는다") {
                val active = Account.createDraft(club = club, cardinal = 5)
                active.updateBasicInfo("5기 회비", Money.of(50_000), null)
                active.activate()
                every { accountRepository.findByIdWithLock(1L) } returns active

                shouldThrow<AccountInvalidDraftStateException> {
                    useCase.discardDraft(clubId = clubId, accountId = 1L, userId = userId)
                }
                verify(exactly = 0) { accountRepository.delete(any()) }
            }
        }

        describe("saveBasic") {
            it("잠금 조회한 장부의 기본 정보와 마지막 수정자를 저장한다") {
                val account = Account.createDraft(club = club, cardinal = 5)
                every { accountRepository.findByIdWithLock(1L) } returns account

                useCase.saveBasic(
                    clubId = clubId,
                    accountId = 1L,
                    request = SaveAccountBasicRequest(name = "5기 정기 회비", duesAmount = 30_000, description = "운영비"),
                    userId = userId,
                )

                account.name shouldBe "5기 정기 회비"
                account.duesAmount shouldBe 30_000
                account.description shouldBe "운영비"
                account.lastModifiedBy shouldBe userId
                account.registrationStep shouldBe AccountRegistrationStep.PAYMENT_TARGETS
            }

            it("장부가 없으면 AccountNotFoundException을 던진다") {
                every { accountRepository.findByIdWithLock(404L) } returns null

                shouldThrow<AccountNotFoundException> {
                    useCase.saveBasic(
                        clubId = clubId,
                        accountId = 404L,
                        request = SaveAccountBasicRequest(name = "5기 정기 회비", duesAmount = 30_000, description = "운영비"),
                        userId = userId,
                    )
                }
            }

            it("다른 동아리 장부이면 AccountNotFoundException을 던지고 수정하지 않는다") {
                val otherClub = ClubTestFixture.createClub(id = 2L, code = "OTHER-CLUB")
                val account = Account.createDraft(club = otherClub, cardinal = 5)
                every { accountRepository.findByIdWithLock(1L) } returns account

                shouldThrow<AccountNotFoundException> {
                    useCase.saveBasic(
                        clubId = clubId,
                        accountId = 1L,
                        request = SaveAccountBasicRequest(name = "5기 정기 회비", duesAmount = 30_000, description = "운영비"),
                        userId = userId,
                    )
                }

                account.name shouldBe null
                account.lastModifiedBy shouldBe null
            }
        }

        describe("savePaymentTargets") {
            // 해당 기수 명부를 ClubMemberCardinal 목록으로 반환하도록 스텁한다.
            fun stubRoster(vararg members: ClubMember) {
                val cardinal = CardinalTestFixture.createCardinal(club = club, cardinalNumber = 5)
                every {
                    clubMemberCardinalReader.findAllByClubIdAndCardinalNumber(clubId, 5, MemberStatus.ACTIVE)
                } returns members.map { ClubMemberCardinalTestFixture.create(it, cardinal) }
            }

            it("초기 등록 - 기존 행이 없으면 선택 멤버를 신규 납부 대상으로 생성하고 다음 단계로 이동한다") {
                val account = Account.createDraft(club = club, cardinal = 5)
                account.updateBasicInfo("5기 회비", Money.of(30_000), "운영비")
                val member1 = ClubMemberTestFixture.createActiveMember(id = 1L, club = club)
                val member2 = ClubMemberTestFixture.createActiveMember(id = 2L, club = club)
                val saveAllSlot = slot<Iterable<AccountPaymentTarget>>()

                every { accountRepository.findByIdWithLock(accountId) } returns account
                stubRoster(member1, member2)
                every {
                    paymentTargetRepository.findAllByAccountIdAndClubMemberIdIn(accountId, any())
                } returns emptyList()
                every { paymentTargetRepository.saveAll(capture(saveAllSlot)) } answers
                    { firstArg<Iterable<AccountPaymentTarget>>().toList() }

                useCase.savePaymentTargets(
                    clubId = clubId,
                    accountId = accountId,
                    request = SavePaymentTargetsRequest(targetedClubMemberIds = listOf(2L, 1L)),
                    userId = userId,
                )

                saveAllSlot.captured.map { it.clubMember.id } shouldContainExactly listOf(1L, 2L)
                saveAllSlot.captured.map { it.dueAmount } shouldContainExactly listOf(30_000, 30_000)
                account.registrationStep shouldBe AccountRegistrationStep.CARRY_OVER
                account.lastModifiedBy shouldBe userId
            }

            it("재설정 - 대상 목록 멤버는 납부 대상으로, 제외 목록의 기존 멤버는 제외 대상으로 갱신한다") {
                val account = Account.createDraft(club = club, cardinal = 5)
                account.updateBasicInfo("5기 회비", Money.of(30_000), "운영비")
                val member1 = ClubMemberTestFixture.createActiveMember(id = 1L, club = club)
                val member2 = ClubMemberTestFixture.createActiveMember(id = 2L, club = club)
                val member3 = ClubMemberTestFixture.createActiveMember(id = 3L, club = club)
                val existingTarget = AccountPaymentTarget.createTargeted(account, member1, Money.of(30_000))
                existingTarget.markPaid(
                    Money.of(30_000),
                    confirmedBy = userId,
                    paidAt = LocalDateTime.of(2026, 3, 1, 10, 0),
                )
                val existingTargeted2 = AccountPaymentTarget.createTargeted(account, member2, Money.of(30_000))
                val saveAllSlot = slot<Iterable<AccountPaymentTarget>>()

                every { accountRepository.findByIdWithLock(accountId) } returns account
                stubRoster(member1, member2, member3)
                every {
                    paymentTargetRepository.findAllByAccountIdAndClubMemberIdIn(accountId, listOf(1L, 3L, 2L))
                } returns listOf(existingTarget, existingTargeted2)
                every { paymentTargetRepository.saveAll(capture(saveAllSlot)) } answers
                    { firstArg<Iterable<AccountPaymentTarget>>().toList() }

                useCase.savePaymentTargets(
                    clubId = clubId,
                    accountId = accountId,
                    request =
                        SavePaymentTargetsRequest(
                            targetedClubMemberIds = listOf(3L, 1L),
                            excludedClubMemberIds = listOf(2L),
                        ),
                    userId = userId,
                )

                existingTarget.targetStatus shouldBe AccountTargetStatus.TARGETED
                existingTarget.paymentStatus shouldBe AccountPaymentStatus.PAID
                existingTargeted2.targetStatus shouldBe AccountTargetStatus.EXCLUDED
                saveAllSlot.captured.map { it.clubMember.id } shouldContainExactly listOf(3L)
                saveAllSlot.captured.first().dueAmount shouldBe 30_000
                account.lastModifiedBy shouldBe userId
                verify(exactly = 1) { clubPermissionPolicy.requireAdmin(clubId, userId) }
            }

            it("델타 - 두 목록에 모두 없는 멤버의 기존 행은 조회하지도 갱신하지도 않는다") {
                val account = Account.createDraft(club = club, cardinal = 5)
                account.updateBasicInfo("5기 회비", Money.of(30_000), "운영비")
                val member1 = ClubMemberTestFixture.createActiveMember(id = 1L, club = club)
                val saveAllSlot = slot<Iterable<AccountPaymentTarget>>()

                every { accountRepository.findByIdWithLock(accountId) } returns account
                stubRoster(member1)
                every {
                    paymentTargetRepository.findAllByAccountIdAndClubMemberIdIn(accountId, listOf(1L))
                } returns emptyList()
                every { paymentTargetRepository.saveAll(capture(saveAllSlot)) } answers
                    { firstArg<Iterable<AccountPaymentTarget>>().toList() }

                useCase.savePaymentTargets(
                    clubId = clubId,
                    accountId = accountId,
                    request = SavePaymentTargetsRequest(targetedClubMemberIds = listOf(1L)),
                    userId = userId,
                )

                saveAllSlot.captured.map { it.clubMember.id } shouldContainExactly listOf(1L)
                verify(exactly = 1) {
                    paymentTargetRepository.findAllByAccountIdAndClubMemberIdIn(accountId, listOf(1L))
                }
                verify(exactly = 0) { paymentTargetRepository.findAllByAccountId(any()) }
            }

            it("빈 요청이면 대상 갱신 없이 다음 단계로만 이동한다") {
                val account = Account.createDraft(club = club, cardinal = 5)
                account.updateBasicInfo("5기 회비", Money.of(30_000), "운영비")

                every { accountRepository.findByIdWithLock(accountId) } returns account

                useCase.savePaymentTargets(
                    clubId = clubId,
                    accountId = accountId,
                    request = SavePaymentTargetsRequest(),
                    userId = userId,
                )

                account.registrationStep shouldBe AccountRegistrationStep.CARRY_OVER
                account.lastModifiedBy shouldBe userId
                verify(exactly = 0) { paymentTargetRepository.findAllByAccountIdAndClubMemberIdIn(any(), any()) }
                verify(exactly = 0) { paymentTargetRepository.saveAll(any<Iterable<AccountPaymentTarget>>()) }
            }

            it("납부 완료된 기존 대상은 제외할 수 없다") {
                val account = Account.createDraft(club = club, cardinal = 5)
                account.updateBasicInfo("5기 회비", Money.of(30_000), "운영비")
                val member = ClubMemberTestFixture.createActiveMember(id = 1L, club = club)
                val existingTarget = AccountPaymentTarget.createTargeted(account, member, Money.of(30_000))
                existingTarget.markPaid(
                    Money.of(30_000),
                    confirmedBy = userId,
                    paidAt = LocalDateTime.of(2026, 3, 1, 10, 0),
                )

                every { accountRepository.findByIdWithLock(accountId) } returns account
                stubRoster(member)
                every {
                    paymentTargetRepository.findAllByAccountIdAndClubMemberIdIn(accountId, listOf(1L))
                } returns listOf(existingTarget)

                shouldThrow<AccountPaymentTargetPaidException> {
                    useCase.savePaymentTargets(
                        clubId = clubId,
                        accountId = accountId,
                        request = SavePaymentTargetsRequest(excludedClubMemberIds = listOf(1L)),
                        userId = userId,
                    )
                }
            }

            it("기수 명부에 없는 멤버가 포함되면 AccountPaymentTargetMemberInvalidException을 던진다") {
                val account = Account.createDraft(club = club, cardinal = 5)
                account.updateBasicInfo("5기 회비", Money.of(30_000), "운영비")
                val member = ClubMemberTestFixture.createActiveMember(id = 1L, club = club)

                every { accountRepository.findByIdWithLock(accountId) } returns account
                stubRoster(member)

                shouldThrow<AccountPaymentTargetMemberInvalidException> {
                    useCase.savePaymentTargets(
                        clubId = clubId,
                        accountId = accountId,
                        request = SavePaymentTargetsRequest(targetedClubMemberIds = listOf(1L, 2L)),
                        userId = userId,
                    )
                }
            }

            it("같은 멤버가 대상/제외 목록에 동시에 포함되면 AccountPaymentTargetMemberInvalidException을 던진다") {
                val account = Account.createDraft(club = club, cardinal = 5)
                account.updateBasicInfo("5기 회비", Money.of(30_000), "운영비")

                every { accountRepository.findByIdWithLock(accountId) } returns account

                shouldThrow<AccountPaymentTargetMemberInvalidException> {
                    useCase.savePaymentTargets(
                        clubId = clubId,
                        accountId = accountId,
                        request =
                            SavePaymentTargetsRequest(
                                targetedClubMemberIds = listOf(1L, 2L),
                                excludedClubMemberIds = listOf(2L),
                            ),
                        userId = userId,
                    )
                }
            }
        }

        describe("saveCarryOver") {
            it("이월 설정과 마지막 수정자를 저장한다") {
                val account = Account.createDraft(club = club, cardinal = 5)
                every { accountRepository.findByIdWithLock(1L) } returns account

                useCase.saveCarryOver(
                    clubId = clubId,
                    accountId = 1L,
                    request = SaveAccountCarryOverRequest(enabled = true, amount = 152_129, memo = "4기 잔액"),
                    userId = userId,
                )

                account.carryOverAmount shouldBe 152_129
                account.carryOverMemo shouldBe "4기 잔액"
                account.lastModifiedBy shouldBe userId
            }
        }

        describe("saveBankAccount") {
            it("계좌 설정과 마지막 수정자를 저장한다") {
                val account = Account.createDraft(club = club, cardinal = 5)
                every { accountRepository.findByIdWithLock(1L) } returns account

                useCase.saveBankAccount(
                    clubId = clubId,
                    accountId = 1L,
                    request =
                        SaveAccountBankAccountRequest(
                            bankAccountVisible = true,
                            bankAccount =
                                BankAccountRequest(
                                    bankName = "국민은행",
                                    accountNumber = "12-12412-1231",
                                    holder = "가천대 검도부",
                                ),
                        ),
                    userId = userId,
                )

                account.bankAccount?.bankName shouldBe "국민은행"
                account.bankAccountVisible shouldBe true
                account.lastModifiedBy shouldBe userId
            }
        }

        describe("completeRegistration") {
            it("초안 작성 중 탈퇴/퇴출된 멤버의 미납 대상 행은 제외 처리하고 활성화한다") {
                val account = Account.createDraft(club = club, cardinal = 5)
                account.updateBasicInfo("5기 회비", Money.of(30_000), "운영비")
                account.updateBankAccount(bankAccount = null, visible = false)
                val bannedMember = ClubMemberTestFixture.createBannedMember(id = 1L, club = club)
                val ghostTarget = AccountPaymentTarget.createTargeted(account, bannedMember, Money.of(30_000))
                every { accountRepository.findByIdWithLock(1L) } returns account
                every {
                    accountRepository.findTopByClubIdAndCardinalLessThanAndStatusOrderByCardinalDesc(
                        clubId = clubId,
                        cardinal = 5,
                        status = AccountStatus.ACTIVE,
                    )
                } returns null
                every { paymentTargetRepository.findAllUnpaidTargetsWithInactiveClubMemberByAccountId(1L) } returns
                    listOf(ghostTarget)

                useCase.completeRegistration(clubId = clubId, accountId = 1L, userId = userId)

                account.status shouldBe AccountStatus.ACTIVE
                ghostTarget.targetStatus shouldBe AccountTargetStatus.EXCLUDED
            }

            it("모든 단계를 저장하지 않은 초안은 완료할 수 없다") {
                val account = Account.createDraft(club = club, cardinal = 5)
                account.updateBasicInfo("5기 회비", Money.of(30_000), "운영비")

                every { accountRepository.findByIdWithLock(1L) } returns account

                shouldThrow<AccountRegistrationStepIncompleteException> {
                    useCase.completeRegistration(clubId = clubId, accountId = 1L, userId = userId)
                }
                account.status shouldBe AccountStatus.DRAFT
            }

            it("이미 활성화된 장부에 완료를 재요청하면 AccountInvalidDraftStateException을 던진다") {
                val account = Account.createDraft(club = club, cardinal = 5)
                account.updateBasicInfo("5기 회비", Money.of(30_000), "운영비")
                account.updateBankAccount(bankAccount = null, visible = false)
                account.activate()

                every { accountRepository.findByIdWithLock(1L) } returns account

                shouldThrow<AccountInvalidDraftStateException> {
                    useCase.completeRegistration(clubId = clubId, accountId = 1L, userId = userId)
                }
            }

            it("이월 금액이 완료 시점의 이전 장부 잔액과 다르면 AccountCarryOverAmountMismatchException을 던진다") {
                val account = Account.createDraft(club = club, cardinal = 5)
                account.updateBasicInfo("5기 회비", Money.of(30_000), "운영비")
                account.updateCarryOver(enabled = true, amount = Money.of(240_000), memo = "3기 잔액")
                account.updateBankAccount(bankAccount = null, visible = false)
                val previousAccount =
                    AccountTestFixture.createAccount(
                        id = 9L,
                        club = club,
                        cardinal = 3,
                        currentAmount = 200_000,
                        currentBalance = 200_000,
                    )

                every { accountRepository.findByIdWithLock(1L) } returns account
                every {
                    accountRepository.findTopByClubIdAndCardinalLessThanAndStatusOrderByCardinalDesc(
                        clubId = clubId,
                        cardinal = 5,
                        status = AccountStatus.ACTIVE,
                    )
                } returns previousAccount
                every { accountRepository.findByIdWithLock(9L) } returns previousAccount

                shouldThrow<AccountCarryOverAmountMismatchException> {
                    useCase.completeRegistration(clubId = clubId, accountId = 1L, userId = userId)
                }
                account.status shouldBe AccountStatus.DRAFT
                previousAccount.currentBalance shouldBe 200_000
                verify(exactly = 0) { transactionRepository.save(any()) }
            }

            it("이월하기로 완료하면 신규 장부에 이월 수입을 기록하고 이전 장부 잔액을 전출 지출로 정리한다") {
                val account = Account.createDraft(club = club, cardinal = 5)
                account.updateBasicInfo("5기 회비", Money.of(30_000), "운영비")
                account.updateCarryOver(enabled = true, amount = Money.of(240_000), memo = "3기 잔액")
                account.updateBankAccount(bankAccount = null, visible = false)
                val previousAccount =
                    AccountTestFixture.createAccount(
                        id = 9L,
                        club = club,
                        cardinal = 3,
                        currentAmount = 240_000,
                        currentBalance = 240_000,
                    )

                every { accountRepository.findByIdWithLock(1L) } returns account
                every { paymentTargetRepository.findAllUnpaidTargetsWithInactiveClubMemberByAccountId(1L) } returns
                    emptyList()
                every {
                    accountRepository.findTopByClubIdAndCardinalLessThanAndStatusOrderByCardinalDesc(
                        clubId = clubId,
                        cardinal = 5,
                        status = AccountStatus.ACTIVE,
                    )
                } returns previousAccount
                every { accountRepository.findByIdWithLock(9L) } returns previousAccount
                every { transactionRepository.save(any()) } answers { firstArg() }

                useCase.completeRegistration(clubId = clubId, accountId = 1L, userId = userId)

                account.status shouldBe AccountStatus.ACTIVE
                account.currentBalance shouldBe 240_000
                previousAccount.currentBalance shouldBe 0
                verify(exactly = 1) {
                    transactionRepository.save(
                        match {
                            it.type == AccountTransactionType.CARRY_OVER &&
                                it.amount == 240_000 &&
                                it.account === account &&
                                // 이월 수입의 거래처는 직전 기수
                                it.source == "3기 회비"
                        },
                    )
                }
                verify(exactly = 1) {
                    transactionRepository.save(
                        match {
                            it.type == AccountTransactionType.EXPENSE &&
                                it.amount == 240_000 &&
                                it.account === previousAccount &&
                                // 전출 지출의 거래처는 신규 기수
                                it.source == "5기 회비"
                        },
                    )
                }
            }

            it("이월하지 않기로 완료하면 이전 기수 장부의 잔액을 지출 거래로 자동 정리한다") {
                val account = Account.createDraft(club = club, cardinal = 5)
                account.updateBasicInfo("5기 회비", Money.of(30_000), "운영비")
                account.updateCarryOver(enabled = false, amount = null, memo = null)
                account.updateBankAccount(bankAccount = null, visible = false)
                val previousAccount =
                    AccountTestFixture.createAccount(
                        id = 9L,
                        club = club,
                        cardinal = 3,
                        currentAmount = 240_000,
                        currentBalance = 240_000,
                    )

                every { accountRepository.findByIdWithLock(1L) } returns account
                every { paymentTargetRepository.findAllUnpaidTargetsWithInactiveClubMemberByAccountId(1L) } returns
                    emptyList()
                every {
                    accountRepository.findTopByClubIdAndCardinalLessThanAndStatusOrderByCardinalDesc(
                        clubId = clubId,
                        cardinal = 5,
                        status = AccountStatus.ACTIVE,
                    )
                } returns previousAccount
                every { accountRepository.findByIdWithLock(9L) } returns previousAccount
                every { transactionRepository.save(any()) } answers { firstArg() }

                useCase.completeRegistration(clubId = clubId, accountId = 1L, userId = userId)

                account.status shouldBe AccountStatus.ACTIVE
                previousAccount.currentBalance shouldBe 0
                verify(exactly = 1) {
                    transactionRepository.save(
                        match {
                            it.type == AccountTransactionType.EXPENSE &&
                                it.amount == 240_000 &&
                                it.account === previousAccount &&
                                // 미이월 정리는 실제 이체처가 없어 거래처를 비운다
                                it.source == null
                        },
                    )
                }
            }

            it("이월하지 않기여도 이전 기수 장부가 없으면 지출 거래를 만들지 않는다") {
                val account = Account.createDraft(club = club, cardinal = 5)
                account.updateBasicInfo("5기 회비", Money.of(30_000), "운영비")
                account.updateCarryOver(enabled = false, amount = null, memo = null)
                account.updateBankAccount(bankAccount = null, visible = false)

                every { accountRepository.findByIdWithLock(1L) } returns account
                every { paymentTargetRepository.findAllUnpaidTargetsWithInactiveClubMemberByAccountId(1L) } returns
                    emptyList()
                every {
                    accountRepository.findTopByClubIdAndCardinalLessThanAndStatusOrderByCardinalDesc(
                        clubId = clubId,
                        cardinal = 5,
                        status = AccountStatus.ACTIVE,
                    )
                } returns null

                useCase.completeRegistration(clubId = clubId, accountId = 1L, userId = userId)

                account.status shouldBe AccountStatus.ACTIVE
                verify(exactly = 0) { transactionRepository.save(any()) }
            }

            it("초안 장부를 활성화하고 이월액이 있으면 CARRY_OVER 거래로 잔액에 반영한다") {
                val account = Account.createDraft(club = club, cardinal = 5)
                account.updateBasicInfo("5기 회비", Money.of(30_000), "운영비")
                account.updateCarryOver(enabled = true, amount = Money.of(152_129), memo = "4기 잔액")
                account.updateBankAccount(bankAccount = null, visible = false)
                every { accountRepository.findByIdWithLock(1L) } returns account
                every {
                    accountRepository.findTopByClubIdAndCardinalLessThanAndStatusOrderByCardinalDesc(
                        clubId = clubId,
                        cardinal = 5,
                        status = AccountStatus.ACTIVE,
                    )
                } returns null
                every { paymentTargetRepository.findAllUnpaidTargetsWithInactiveClubMemberByAccountId(1L) } returns
                    emptyList()
                every { transactionRepository.save(any()) } answers { firstArg() }

                useCase.completeRegistration(clubId = clubId, accountId = 1L, userId = userId)

                account.status shouldBe AccountStatus.ACTIVE
                account.currentBalance shouldBe 152_129
                account.lastModifiedBy shouldBe userId
                verify(exactly = 1) {
                    transactionRepository.save(
                        match {
                            it.type == AccountTransactionType.CARRY_OVER &&
                                it.amount == 152_129 &&
                                it.isApplied
                        },
                    )
                }
            }
        }
    })
