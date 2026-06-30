package com.weeth.domain.account.application.usecase.command

import com.ninjasquad.springmockk.MockkBean
import com.weeth.config.TestContainersConfig
import com.weeth.domain.account.application.dto.request.BankAccountRequest
import com.weeth.domain.account.application.dto.request.SaveAccountBankAccountRequest
import com.weeth.domain.account.application.dto.request.SaveAccountBasicRequest
import com.weeth.domain.account.application.dto.request.SaveAccountCarryOverRequest
import com.weeth.domain.account.application.dto.request.SavePaymentTargetsRequest
import com.weeth.domain.account.application.exception.AccountCarryOverAmountMismatchException
import com.weeth.domain.account.application.exception.AccountErrorCode
import com.weeth.domain.account.application.exception.AccountInvalidDraftStateException
import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.application.exception.AccountPaymentTargetMemberInvalidException
import com.weeth.domain.account.application.exception.AccountPaymentTargetPaidException
import com.weeth.domain.account.application.exception.AccountRegistrationStepIncompleteException
import com.weeth.domain.account.application.usecase.query.GetAccountPaymentTargetQueryService
import com.weeth.domain.account.application.usecase.query.GetAccountRegistrationQueryService
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.entity.AccountPaymentTarget
import com.weeth.domain.account.domain.entity.AccountTransaction
import com.weeth.domain.account.domain.enums.AccountPaymentStatus
import com.weeth.domain.account.domain.enums.AccountRegistrationStep
import com.weeth.domain.account.domain.enums.AccountStatus
import com.weeth.domain.account.domain.enums.AccountTargetStatus
import com.weeth.domain.account.domain.enums.AccountTransactionType
import com.weeth.domain.account.domain.repository.AccountPaymentTargetRepository
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.account.domain.repository.AccountTransactionRepository
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.domain.repository.CardinalRepository
import com.weeth.domain.club.application.exception.NotClubAdminException
import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberCardinalRepository
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.enums.Status
import com.weeth.domain.user.domain.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfig::class)
class MembershipFeeRegistrationIntegrationTest(
    private val registerAccountUseCase: RegisterAccountUseCase,
    private val registrationQueryService: GetAccountRegistrationQueryService,
    private val paymentTargetQueryService: GetAccountPaymentTargetQueryService,
    private val accountRepository: AccountRepository,
    private val paymentTargetRepository: AccountPaymentTargetRepository,
    private val transactionRepository: AccountTransactionRepository,
    private val clubRepository: ClubRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val clubMemberCardinalRepository: ClubMemberCardinalRepository,
    private val cardinalRepository: CardinalRepository,
    private val userRepository: UserRepository,
    @MockkBean private val fileAccessUrlPort: FileAccessUrlPort,
) : DescribeSpec() {
    init {
        beforeTest {
            every { fileAccessUrlPort.resolve(any()) } answers { firstArg() }
        }

        describe("회비 등록 플로우 통합 시나리오") {
            it("S1. 이전 장부가 있는 상태에서 이월하기로 등록을 완료한다") {
                val context = createContext("S1", previousBalance = 240_000)
                val accountId = context.createDraft()
                context.saveBasicAndTargets(
                    accountId,
                    targeted = context.memberIds(0, 1),
                )

                registrationQueryService
                    .findCarryOverSource(context.club.id, accountId, context.adminUser.id)
                    .let {
                        it.hasPreviousAccount shouldBe true
                        it.cardinalNumber shouldBe 3
                        it.balance shouldBe 240_000
                    }

                context.saveCarryOverAndBank(accountId, enabled = true, amount = 240_000)
                registerAccountUseCase.completeRegistration(context.club.id, accountId, context.adminUser.id)

                val account = accountRepository.findById(accountId).orElseThrow()
                val previous = accountRepository.findById(context.previousAccount!!.id).orElseThrow()
                account.status shouldBe AccountStatus.ACTIVE
                account.currentBalance shouldBe 240_000
                account.lastModifiedBy shouldBe context.adminUser.id
                previous.currentBalance shouldBe 0
                transactions(accountId, AccountTransactionType.CARRY_OVER).single().let {
                    it.amount shouldBe 240_000
                    it.isApplied shouldBe true
                }
                transactions(previous.id, AccountTransactionType.EXPENSE).single().let {
                    it.amount shouldBe 240_000
                    it.title shouldBe "이월 잔액 전출"
                }
                paymentTargets(accountId, context.memberIds(0, 1)).forEach {
                    it.targetStatus shouldBe AccountTargetStatus.TARGETED
                    it.paymentStatus shouldBe AccountPaymentStatus.UNPAID
                    it.dueAmount shouldBe 30_000
                }
                targetResponses(context, accountId).byMember(context.members[2].id).targetStatus shouldBe
                    AccountTargetStatus.EXCLUDED
            }

            it("S2/S3. 이전 장부 없음 수동 이월과 이월하지 않기 자동 마감을 검증한다") {
                val first = createContext("S2", previousBalance = null)
                val firstAccountId = first.createDraft()
                first.saveBasicAndTargets(
                    firstAccountId,
                    targeted = first.memberIds(0, 1),
                )
                registrationQueryService.findCarryOverSource(first.club.id, firstAccountId, first.adminUser.id).let {
                    it.hasPreviousAccount shouldBe false
                    it.balance.shouldBeNull()
                }
                first.saveCarryOverAndBank(firstAccountId, enabled = true, amount = 100_000)
                registerAccountUseCase.completeRegistration(first.club.id, firstAccountId, first.adminUser.id)
                accountRepository.findById(firstAccountId).orElseThrow().currentBalance shouldBe 100_000
                transactions(firstAccountId, AccountTransactionType.CARRY_OVER).single().amount shouldBe 100_000
                transactions(firstAccountId, AccountTransactionType.EXPENSE).size shouldBe 0

                val noCarry = createContext("S3", previousBalance = 240_000)
                val noCarryAccountId = noCarry.createDraft()
                noCarry.saveBasicAndTargets(noCarryAccountId, targeted = noCarry.memberIds(0))
                noCarry.saveCarryOverAndBank(noCarryAccountId, enabled = false, amount = null)
                registerAccountUseCase.completeRegistration(noCarry.club.id, noCarryAccountId, noCarry.adminUser.id)
                accountRepository.findById(noCarryAccountId).orElseThrow().currentBalance shouldBe 0
                val previous = accountRepository.findById(noCarry.previousAccount!!.id).orElseThrow()
                previous.currentBalance shouldBe 0
                transactions(previous.id, AccountTransactionType.EXPENSE).single().title shouldBe "미이월 잔액 정리"
                transactions(noCarryAccountId, AccountTransactionType.CARRY_OVER).size shouldBe 0
            }

            it("S4. 작성 중인 초안은 이어서 작성 정보와 체크 상태를 복원한다") {
                val context = createContext("S4", previousBalance = 240_000)
                val accountId = context.createDraft()
                context.saveBasicAndTargets(
                    accountId,
                    targeted = context.memberIds(0, 1),
                )

                val resumed =
                    registerAccountUseCase.createDraft(
                        context.club.id,
                        cardinal = 5,
                        userId = context.adminUser.id,
                    )
                resumed.isNew shouldBe false
                resumed.accountId shouldBe accountId
                resumed.lastModifiedByName shouldBe context.adminUser.name

                val status = registrationQueryService.findStatus(context.club.id, accountId, context.adminUser.id)
                status.registrationStep shouldBe AccountRegistrationStep.CARRY_OVER
                status.basic.shouldNotBeNull().name shouldBe "5기 정기 회비"
                status.carryOver.shouldBeNull()
                status.bankAccount.shouldBeNull()
                status.paymentTargets.shouldNotBeNull().targetCount shouldBe 2
                status.paymentTargets.shouldNotBeNull().excludedCount shouldBe 2
                targetResponses(context, accountId).let {
                    it.byMember(context.members[0].id).targetStatus shouldBe AccountTargetStatus.TARGETED
                    it.byMember(context.members[2].id).targetStatus shouldBe AccountTargetStatus.EXCLUDED
                }
            }

            it("S5. 납부 대상이 있는 초안을 폐기하고 같은 기수를 새 초안으로 다시 생성한다") {
                val context = createContext("S5", previousBalance = null)
                val oldAccountId = context.createDraft()
                context.saveBasicAndTargets(oldAccountId, targeted = context.memberIds(0, 1))
                paymentTargetRepository.findAllByAccountId(oldAccountId).size shouldBe 2

                registerAccountUseCase.discardDraft(context.club.id, oldAccountId, context.adminUser.id)

                accountRepository.findById(oldAccountId).isPresent shouldBe false
                paymentTargetRepository.findAllByAccountId(oldAccountId).size shouldBe 0
                val newDraft =
                    registerAccountUseCase.createDraft(
                        context.club.id,
                        cardinal = 5,
                        userId = context.adminUser.id,
                    )
                newDraft.isNew shouldBe true
                newDraft.accountId shouldNotBe oldAccountId

                context.saveBasicAndTargets(newDraft.accountId, targeted = context.memberIds(0))
                context.saveCarryOverAndBank(newDraft.accountId, enabled = false, amount = null)
                registerAccountUseCase.completeRegistration(context.club.id, newDraft.accountId, context.adminUser.id)
                shouldThrow<AccountInvalidDraftStateException> {
                    registerAccountUseCase.discardDraft(context.club.id, newDraft.accountId, context.adminUser.id)
                }.errorCode.code shouldBe AccountErrorCode.ACCOUNT_INVALID_DRAFT_STATE.code
            }

            it("S6/S10. 납부 대상 스냅샷 재설정과 조회 카운트 정합성을 검증한다") {
                val context = createContext("S6-S10", previousBalance = null, activeMemberCount = 18)
                val accountId = context.createDraft()
                registerAccountUseCase.saveBasic(context.club.id, accountId, basicRequest(), context.adminUser.id)
                registerAccountUseCase.savePaymentTargets(
                    context.club.id,
                    accountId,
                    SavePaymentTargetsRequest(targetedClubMemberIds = context.members.take(12).map { it.id }),
                    context.adminUser.id,
                )
                // 스냅샷 재설정: 1번은 선택에서 빼고(제외 전환) 12번을 새로 선택해 최종 선택을 {0, 2..12}로 만든다.
                val secondSelection =
                    context.members.filterIndexed { index, _ -> index != 1 && index <= 12 }.map { it.id }
                registerAccountUseCase.savePaymentTargets(
                    context.club.id,
                    accountId,
                    SavePaymentTargetsRequest(targetedClubMemberIds = secondSelection),
                    context.adminUser.id,
                )

                val all = findTargets(context, accountId, targetStatus = null)
                all.summary.totalCount shouldBe 18
                all.summary.targetedCount shouldBe 12
                all.summary.excludedCount shouldBe 6
                findTargets(
                    context,
                    accountId,
                    targetStatus = AccountTargetStatus.TARGETED,
                ).targets.totalElements shouldBe
                    12
                findTargets(
                    context,
                    accountId,
                    targetStatus = AccountTargetStatus.EXCLUDED,
                ).targets.totalElements shouldBe
                    6
                all.byMember(context.members[0].id).targetStatus shouldBe AccountTargetStatus.TARGETED
                all.byMember(context.members[1].id).targetStatus shouldBe AccountTargetStatus.EXCLUDED
                all.byMember(context.members[11].id).targetStatus shouldBe AccountTargetStatus.TARGETED
                all.byMember(context.members[12].id).targetStatus shouldBe AccountTargetStatus.TARGETED
                findTargets(context, accountId, keyword = "회원01", targetStatus = null).targets.totalElements shouldBe 1
                registrationQueryService
                    .findStatus(context.club.id, accountId, context.adminUser.id)
                    .paymentTargets
                    .shouldNotBeNull()
                    .targetCount shouldBe 12

                // 스냅샷: 빈 선택은 전원 제외를 의미하므로 납부 대상이 0이 된다.
                registerAccountUseCase.savePaymentTargets(
                    context.club.id,
                    accountId,
                    SavePaymentTargetsRequest(),
                    context.adminUser.id,
                )
                findTargets(context, accountId, targetStatus = null).summary.targetedCount shouldBe 0
            }

            it("S7. 납부 대상 검증 실패는 기존 행 상태를 보존한다") {
                val context = createContext("S7", previousBalance = null)
                val accountId = context.createDraft()
                context.saveBasicAndTargets(accountId, targeted = context.memberIds(0, 1))
                val other = createContext("S7-OTHER", previousBalance = null)

                shouldThrow<AccountPaymentTargetMemberInvalidException> {
                    registerAccountUseCase.savePaymentTargets(
                        context.club.id,
                        accountId,
                        SavePaymentTargetsRequest(targetedClubMemberIds = listOf(other.members[0].id)),
                        context.adminUser.id,
                    )
                }.errorCode.code shouldBe AccountErrorCode.ACCOUNT_PAYMENT_TARGET_MEMBER_INVALID.code

                val paidTarget = paymentTargets(accountId, context.memberIds(0)).single()
                paidTarget.markPaid(Money.of(30_000), confirmedBy = context.adminUser.id, paidAt = LocalDateTime.now())
                paymentTargetRepository.save(paidTarget)
                // 스냅샷에서 납부 완료 멤버를 선택에서 빼면 제외 전환이 막힌다(방어 가드).
                shouldThrow<AccountPaymentTargetPaidException> {
                    registerAccountUseCase.savePaymentTargets(
                        context.club.id,
                        accountId,
                        SavePaymentTargetsRequest(targetedClubMemberIds = context.memberIds(1)),
                        context.adminUser.id,
                    )
                }.errorCode.code shouldBe AccountErrorCode.ACCOUNT_PAYMENT_TARGET_ALREADY_PAID.code

                paymentTargets(accountId, context.memberIds(0, 1)).let {
                    it.byMember(context.members[0].id).paymentStatus shouldBe AccountPaymentStatus.PAID
                    it.byMember(context.members[0].id).targetStatus shouldBe AccountTargetStatus.TARGETED
                    it.byMember(context.members[1].id).targetStatus shouldBe AccountTargetStatus.TARGETED
                }
            }

            it("S8. 작성 중 비활성화된 멤버는 조회에서 빠지고 완료 시 제외 처리된다") {
                val context = createContext("S8", previousBalance = null)
                val accountId = context.createDraft()
                context.saveBasicAndTargets(accountId, targeted = context.memberIds(0, 1))
                context.members[1].ban()
                clubMemberRepository.save(context.members[1])

                targetResponses(context, accountId).let {
                    it.summary.totalCount shouldBe 3
                    it.summary.targetedCount shouldBe 1
                    it.targets.content.map { row -> row.paymentTargetInfo.clubMemberId } shouldContainExactlyInAnyOrder
                        context.memberIds(0, 2, 3)
                }
                shouldThrow<AccountPaymentTargetMemberInvalidException> {
                    registerAccountUseCase.savePaymentTargets(
                        context.club.id,
                        accountId,
                        SavePaymentTargetsRequest(targetedClubMemberIds = context.memberIds(1)),
                        context.adminUser.id,
                    )
                }

                context.saveCarryOverAndBank(accountId, enabled = false, amount = null)
                registerAccountUseCase.completeRegistration(context.club.id, accountId, context.adminUser.id)
                paymentTargets(accountId, context.memberIds(1)).single().targetStatus shouldBe
                    AccountTargetStatus.EXCLUDED
            }

            it("S9. 완료 가드와 이월 금액 불일치 복구를 검증한다") {
                val incomplete = createContext("S9-INCOMPLETE", previousBalance = 240_000)
                val incompleteAccountId = incomplete.createDraft()
                registerAccountUseCase.saveBasic(
                    incomplete.club.id,
                    incompleteAccountId,
                    basicRequest(),
                    incomplete.adminUser.id,
                )
                shouldThrow<AccountRegistrationStepIncompleteException> {
                    registerAccountUseCase.completeRegistration(
                        incomplete.club.id,
                        incompleteAccountId,
                        incomplete.adminUser.id,
                    )
                }.errorCode.code shouldBe AccountErrorCode.ACCOUNT_REGISTRATION_STEP_INCOMPLETE.code
                accountRepository.findById(incomplete.previousAccount!!.id).orElseThrow().currentBalance shouldBe
                    240_000

                val mismatch = createContext("S9-MISMATCH", previousBalance = 240_000)
                val accountId = mismatch.createDraft()
                mismatch.saveBasicAndTargets(accountId, targeted = mismatch.memberIds(0))
                mismatch.saveCarryOverAndBank(accountId, enabled = true, amount = 240_000)
                spendPreviousAccount(mismatch.previousAccount!!.id, amount = 40_000)

                shouldThrow<AccountCarryOverAmountMismatchException> {
                    registerAccountUseCase.completeRegistration(mismatch.club.id, accountId, mismatch.adminUser.id)
                }.errorCode.code shouldBe AccountErrorCode.ACCOUNT_CARRY_OVER_AMOUNT_MISMATCH.code
                accountRepository.findById(accountId).orElseThrow().status shouldBe AccountStatus.DRAFT
                transactions(accountId, AccountTransactionType.CARRY_OVER).size shouldBe 0

                registrationQueryService
                    .findCarryOverSource(
                        mismatch.club.id,
                        accountId,
                        mismatch.adminUser.id,
                    ).balance shouldBe
                    200_000
                registerAccountUseCase.saveCarryOver(
                    mismatch.club.id,
                    accountId,
                    SaveAccountCarryOverRequest(enabled = true, amount = 200_000, memo = "수정 이월"),
                    mismatch.adminUser.id,
                )
                registerAccountUseCase.completeRegistration(mismatch.club.id, accountId, mismatch.adminUser.id)
                accountRepository.findById(accountId).orElseThrow().currentBalance shouldBe 200_000
                shouldThrow<AccountInvalidDraftStateException> {
                    registerAccountUseCase.completeRegistration(mismatch.club.id, accountId, mismatch.adminUser.id)
                }.errorCode.code shouldBe AccountErrorCode.ACCOUNT_INVALID_DRAFT_STATE.code
            }

            it("S11. 권한과 장부 소속 경계를 검증한다") {
                val context = createContext("S11", previousBalance = null)
                val accountId = context.createDraft()

                shouldThrow<NotClubAdminException> {
                    registerAccountUseCase.saveBasic(
                        context.club.id,
                        accountId,
                        basicRequest(),
                        context.members[0].user.id,
                    )
                }
                val other = createContext("S11-OTHER", previousBalance = null)
                shouldThrow<AccountNotFoundException> {
                    registerAccountUseCase.saveBasic(other.club.id, accountId, basicRequest(), other.adminUser.id)
                }.errorCode.code shouldBe AccountErrorCode.ACCOUNT_NOT_FOUND.code
                shouldThrow<AccountNotFoundException> {
                    registerAccountUseCase.saveBasic(
                        context.club.id,
                        Long.MAX_VALUE,
                        basicRequest(),
                        context.adminUser.id,
                    )
                }.errorCode.code shouldBe AccountErrorCode.ACCOUNT_NOT_FOUND.code
            }
        }
    }

    fun createContext(
        key: String,
        previousBalance: Int?,
        activeMemberCount: Int = 4,
    ): RegistrationContext {
        val suffix = "$key-${System.nanoTime()}"
        val club =
            clubRepository.save(
                ClubTestFixture.createClub(
                    name = "회비 통합 테스트 $suffix",
                    code = "FEE-$suffix",
                ),
            )
        val adminUser = saveUser("관리자-$suffix", "admin-$suffix@test.com")
        clubMemberRepository.save(ClubMember(club, adminUser, MemberStatus.ACTIVE, MemberRole.ADMIN))
        val members =
            (1..activeMemberCount).map {
                val user = saveUser("회원%02d-$key".format(it), "member-$suffix-$it@test.com")
                clubMemberRepository.save(ClubMember(club, user, MemberStatus.ACTIVE, MemberRole.USER))
            }
        val previousCardinal = cardinalRepository.save(Cardinal.create(club, cardinalNumber = 3))
        val currentCardinal = cardinalRepository.save(Cardinal.create(club, cardinalNumber = 5))
        members.forEach {
            clubMemberCardinalRepository.save(ClubMemberCardinal.create(it, currentCardinal))
        }
        val previousAccount =
            previousBalance?.let {
                accountRepository.save(
                    Account(
                        club = club,
                        totalAmount = it,
                        currentAmount = it,
                        currentBalance = it,
                        cardinal = previousCardinal.cardinalNumber,
                        name = "3기 회비",
                        duesAmount = 30_000,
                        status = AccountStatus.ACTIVE,
                    ),
                )
            }
        return RegistrationContext(club, adminUser, members, previousAccount)
    }

    fun saveUser(
        name: String,
        email: String,
    ): User = userRepository.save(User.create(name = name, email = email, status = Status.ACTIVE))

    fun RegistrationContext.createDraft(): Long =
        registerAccountUseCase
            .createDraft(club.id, cardinal = 5, userId = adminUser.id)
            .also { it.isNew shouldBe true }
            .accountId

    fun RegistrationContext.saveBasicAndTargets(
        accountId: Long,
        targeted: List<Long>,
    ) {
        registerAccountUseCase.saveBasic(club.id, accountId, basicRequest(), adminUser.id)
        accountRepository.findById(accountId).orElseThrow().registrationStep shouldBe
            AccountRegistrationStep.PAYMENT_TARGETS
        registerAccountUseCase.savePaymentTargets(
            club.id,
            accountId,
            SavePaymentTargetsRequest(targetedClubMemberIds = targeted),
            adminUser.id,
        )
        accountRepository.findById(accountId).orElseThrow().registrationStep shouldBe AccountRegistrationStep.CARRY_OVER
    }

    fun RegistrationContext.saveCarryOverAndBank(
        accountId: Long,
        enabled: Boolean,
        amount: Int?,
    ) {
        registerAccountUseCase.saveCarryOver(
            club.id,
            accountId,
            SaveAccountCarryOverRequest(enabled = enabled, amount = amount, memo = amount?.let { "3기 잔액" }),
            adminUser.id,
        )
        accountRepository.findById(accountId).orElseThrow().registrationStep shouldBe
            AccountRegistrationStep.BANK_ACCOUNT
        registerAccountUseCase.saveBankAccount(
            club.id,
            accountId,
            SaveAccountBankAccountRequest(
                bankAccountVisible = true,
                bankAccount = BankAccountRequest("국민은행", "123-456-789", "가천대 검도부"),
            ),
            adminUser.id,
        )
        accountRepository.findById(accountId).orElseThrow().registrationStep shouldBe AccountRegistrationStep.REVIEW
    }

    fun RegistrationContext.memberIds(vararg indices: Int): List<Long> = indices.map { members[it].id }

    fun basicRequest(): SaveAccountBasicRequest =
        SaveAccountBasicRequest(name = "5기 정기 회비", duesAmount = 30_000, description = "운영비")

    fun paymentTargets(
        accountId: Long,
        clubMemberIds: List<Long>,
    ): List<AccountPaymentTarget> =
        paymentTargetRepository.findAllByAccountIdAndClubMemberIdIn(accountId, clubMemberIds)

    fun List<AccountPaymentTarget>.byMember(clubMemberId: Long): AccountPaymentTarget =
        first { it.clubMember.id == clubMemberId }

    fun targetResponses(
        context: RegistrationContext,
        accountId: Long,
    ) = findTargets(context, accountId, keyword = null, targetStatus = null)

    fun findTargets(
        context: RegistrationContext,
        accountId: Long,
        keyword: String? = null,
        targetStatus: AccountTargetStatus?,
    ) = paymentTargetQueryService.findTargets(
        context.club.id,
        accountId,
        context.adminUser.id,
        page = 0,
        size = 100,
        keyword = keyword,
        targetStatus = targetStatus,
    )

    fun com.weeth.domain.account.application.dto.response.AccountPaymentTargetsResponse.byMember(clubMemberId: Long) =
        targets.content.first { it.paymentTargetInfo.clubMemberId == clubMemberId }

    fun transactions(
        accountId: Long,
        type: AccountTransactionType,
    ): List<AccountTransaction> =
        transactionRepository.findAll().filter {
            it.account.id == accountId &&
                it.type == type
        }

    fun spendPreviousAccount(
        previousAccountId: Long,
        amount: Int,
    ) {
        val previous = accountRepository.findById(previousAccountId).orElseThrow()
        val expense =
            AccountTransaction.create(
                account = previous,
                type = AccountTransactionType.EXPENSE,
                title = "완료 직전 지출",
                source = null,
                amount = Money.of(amount),
                transactedAt = LocalDateTime.now(),
            )
        previous.applyTransaction(expense)
        transactionRepository.save(expense)
        accountRepository.save(previous)
    }

    data class RegistrationContext(
        val club: Club,
        val adminUser: User,
        val members: List<ClubMember>,
        val previousAccount: Account?,
    )
}
