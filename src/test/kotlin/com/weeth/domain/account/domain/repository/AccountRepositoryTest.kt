package com.weeth.domain.account.domain.repository

import com.weeth.config.TestContainersConfig
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.entity.AccountPaymentTarget
import com.weeth.domain.account.domain.entity.AccountTransaction
import com.weeth.domain.account.domain.enums.AccountPaymentStatus
import com.weeth.domain.account.domain.enums.AccountStatus
import com.weeth.domain.account.domain.enums.AccountTargetStatus
import com.weeth.domain.account.domain.enums.AccountTransactionType
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.domain.repository.CardinalRepository
import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberCardinalRepository
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest
@Import(TestContainersConfig::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccountRepositoryTest(
    private val accountRepository: AccountRepository,
    private val accountTransactionRepository: AccountTransactionRepository,
    private val accountPaymentTargetRepository: AccountPaymentTargetRepository,
    private val clubRepository: ClubRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val cardinalRepository: CardinalRepository,
    private val clubMemberCardinalRepository: ClubMemberCardinalRepository,
    private val userRepository: UserRepository,
    private val entityManager: TestEntityManager,
) : DescribeSpec({
        fun createUser(
            name: String,
            email: String,
        ): User =
            userRepository.save(
                User.create(name = name, email = email, status = com.weeth.domain.user.domain.enums.Status.ACTIVE),
            )

        fun createMember(
            club: Club,
            name: String,
            email: String,
            status: MemberStatus = MemberStatus.ACTIVE,
        ): ClubMember =
            clubMemberRepository.save(
                ClubMemberTestFixture.createActiveMember(club = club, user = createUser(name, email)).also {
                    if (status == MemberStatus.BANNED) it.ban()
                    if (status == MemberStatus.LEFT) it.leave(LocalDateTime.of(2026, 3, 1, 10, 0))
                },
            )

        fun assignCardinal(
            clubMember: ClubMember,
            cardinal: Cardinal,
        ) {
            clubMemberCardinalRepository.save(ClubMemberCardinal.create(clubMember, cardinal))
        }

        fun createActiveAccount(
            club: Club,
            cardinal: Int,
            memberVisible: Boolean = false,
            currentBalance: Int = 0,
        ): Account {
            val account =
                Account(
                    club = club,
                    currentBalance = currentBalance,
                    cardinal = cardinal,
                    name = "${cardinal}기 회비",
                    duesAmount = 50_000,
                    status = AccountStatus.ACTIVE,
                )
            if (memberVisible) account.showToMembers()
            return accountRepository.save(account)
        }

        describe("AccountRepository") {
            it("동아리, 기수, 상태로 회비 장부를 조회한다") {
                val club = clubRepository.save(ClubTestFixture.createClub(code = "ACCOUNT-REPO-1"))
                val draft = accountRepository.save(Account.createDraft(club = club, cardinal = 4))

                val found =
                    accountRepository.findByClubIdAndCardinalAndStatus(
                        clubId = club.id,
                        cardinal = 4,
                        status = AccountStatus.DRAFT,
                    )

                found?.id shouldBe draft.id
                accountRepository.existsByClubIdAndCardinalAndStatus(club.id, 4, AccountStatus.DRAFT) shouldBe true
                accountRepository.existsByClubIdAndCardinalAndStatus(club.id, 4, AccountStatus.ACTIVE) shouldBe false
            }

            it("같은 동아리와 기수에는 장부를 하나만 저장할 수 있다") {
                val club = clubRepository.save(ClubTestFixture.createClub(code = "ACCOUNT-REPO-UNIQUE"))
                accountRepository.saveAndFlush(Account.createDraft(club = club, cardinal = 7))

                shouldThrow<DataIntegrityViolationException> {
                    accountRepository.saveAndFlush(Account.createDraft(club = club, cardinal = 7))
                }
            }

            it("회원 공개된 활성 장부만 기수로 조회한다") {
                val visibleClub = clubRepository.save(ClubTestFixture.createClub(code = "ACCOUNT-REPO-VISIBLE"))
                val hiddenClub =
                    clubRepository.save(
                        ClubTestFixture.createClub(name = "비공개 장부 테스트 동아리", code = "ACCOUNT-REPO-HIDDEN"),
                    )
                val visibleAccount = createActiveAccount(visibleClub, cardinal = 6, memberVisible = true)
                createActiveAccount(hiddenClub, cardinal = 6, memberVisible = false)

                val found =
                    accountRepository.findByClubIdAndCardinalAndStatusAndMemberVisibleTrue(
                        clubId = visibleClub.id,
                        cardinal = 6,
                        status = AccountStatus.ACTIVE,
                    )

                found?.id shouldBe visibleAccount.id
                accountRepository.findByClubIdAndCardinalAndStatusAndMemberVisibleTrue(
                    clubId = hiddenClub.id,
                    cardinal = 6,
                    status = AccountStatus.ACTIVE,
                ) shouldBe null
                accountRepository.findByClubIdAndCardinalAndStatusAndMemberVisibleTrue(
                    clubId = visibleClub.id,
                    cardinal = 6,
                    status = AccountStatus.DRAFT,
                ) shouldBe null
            }

            it("현재 기수보다 작은 직전 활성 장부를 기수 내림차순으로 조회한다") {
                val club = clubRepository.save(ClubTestFixture.createClub(code = "ACCOUNT-REPO-PREVIOUS"))
                createActiveAccount(club, cardinal = 3, currentBalance = 30_000)
                val previous = createActiveAccount(club, cardinal = 5, currentBalance = 50_000)
                accountRepository.save(Account.createDraft(club = club, cardinal = 6))
                createActiveAccount(club, cardinal = 7, currentBalance = 70_000)

                val found =
                    accountRepository.findTopByClubIdAndCardinalLessThanAndStatusOrderByCardinalDesc(
                        clubId = club.id,
                        cardinal = 6,
                        status = AccountStatus.ACTIVE,
                    )

                found?.id shouldBe previous.id
                found?.cardinal shouldBe 5
            }

            it("잠금 조회는 ID에 해당하는 장부를 반환하고 없으면 null을 반환한다") {
                val club = clubRepository.save(ClubTestFixture.createClub(code = "ACCOUNT-REPO-LOCK"))
                val account = accountRepository.save(Account.createDraft(club = club, cardinal = 8))

                accountRepository.findByIdWithLock(account.id)?.id shouldBe account.id
                accountRepository.findByIdWithLock(Long.MAX_VALUE) shouldBe null
            }
        }

        describe("AccountTransactionRepository") {
            it("삭제되지 않은 거래를 타입 묶음 기준으로 집계한다") {
                val club = clubRepository.save(ClubTestFixture.createClub(code = "ACCOUNT-REPO-2"))
                val account = accountRepository.save(Account.createDraft(club = club, cardinal = 5))
                accountTransactionRepository.save(
                    AccountTransaction.create(
                        account = account,
                        type = AccountTransactionType.DUES,
                        title = "5기 회비",
                        source = null,
                        amount = Money.of(50_000),
                        transactedAt = LocalDateTime.of(2026, 3, 13, 10, 0),
                    ),
                )
                accountTransactionRepository.save(
                    AccountTransaction.create(
                        account = account,
                        type = AccountTransactionType.CARRY_OVER,
                        title = "이월금",
                        source = null,
                        amount = Money.of(13_000),
                        transactedAt = LocalDateTime.of(2026, 3, 13, 10, 0),
                    ),
                )
                val deleted =
                    AccountTransaction.create(
                        account = account,
                        type = AccountTransactionType.DUES,
                        title = "삭제된 회비",
                        source = null,
                        amount = Money.of(50_000),
                        transactedAt = LocalDateTime.of(2026, 3, 14, 10, 0),
                    )
                deleted.softDelete()
                accountTransactionRepository.save(deleted)

                val duesLikeCount =
                    accountTransactionRepository.countByAccountIdAndTypeInAndDeletedAtIsNull(
                        accountId = account.id,
                        types = listOf(AccountTransactionType.DUES, AccountTransactionType.CARRY_OVER),
                    )

                duesLikeCount shouldBe 2
            }

            it("거래 집계는 장부와 타입을 함께 필터링한다") {
                val club = clubRepository.save(ClubTestFixture.createClub(code = "ACCOUNT-REPO-TX-FILTER"))
                val account = accountRepository.save(Account.createDraft(club = club, cardinal = 5))
                val otherAccount = accountRepository.save(Account.createDraft(club = club, cardinal = 6))

                accountTransactionRepository.save(
                    AccountTransaction.create(
                        account = account,
                        type = AccountTransactionType.DUES,
                        title = "5기 회비",
                        source = null,
                        amount = Money.of(50_000),
                        transactedAt = LocalDateTime.of(2026, 3, 13, 10, 0),
                    ),
                )
                accountTransactionRepository.save(
                    AccountTransaction.create(
                        account = account,
                        type = AccountTransactionType.EXPENSE,
                        title = "지출",
                        source = null,
                        amount = Money.of(10_000),
                        transactedAt = LocalDateTime.of(2026, 3, 14, 10, 0),
                    ),
                )
                accountTransactionRepository.save(
                    AccountTransaction.create(
                        account = otherAccount,
                        type = AccountTransactionType.DUES,
                        title = "6기 회비",
                        source = null,
                        amount = Money.of(60_000),
                        transactedAt = LocalDateTime.of(2026, 3, 15, 10, 0),
                    ),
                )

                accountTransactionRepository.countByAccountIdAndTypeInAndDeletedAtIsNull(
                    accountId = account.id,
                    types = listOf(AccountTransactionType.DUES),
                ) shouldBe 1
            }

            it("적용된 거래에 그 시점의 총잔액(balanceAfter)이 저장된다") {
                val club = clubRepository.save(ClubTestFixture.createClub(code = "ACCOUNT-REPO-BALANCE-AFTER"))
                val account = accountRepository.save(Account.createDraft(club = club, cardinal = 9))

                fun applyAndSave(
                    type: AccountTransactionType,
                    amount: Int,
                    at: LocalDateTime,
                ): AccountTransaction {
                    val tx =
                        AccountTransaction.create(
                            account = account,
                            type = type,
                            title = "거래",
                            source = null,
                            amount = Money.of(amount),
                            transactedAt = at,
                        )
                    account.applyTransaction(tx)
                    return accountTransactionRepository.save(tx)
                }

                val tx1 = applyAndSave(AccountTransactionType.INCOME, 100_000, LocalDateTime.of(2026, 3, 1, 10, 0))
                val tx2 = applyAndSave(AccountTransactionType.EXPENSE, 30_000, LocalDateTime.of(2026, 3, 2, 10, 0))
                accountRepository.save(account)

                accountTransactionRepository.findByIdAndDeletedAtIsNull(tx1.id)?.balanceAfter shouldBe 100_000
                accountTransactionRepository.findByIdAndDeletedAtIsNull(tx2.id)?.balanceAfter shouldBe 70_000
                account.currentBalance shouldBe 70_000
            }
        }

        describe("AccountPaymentTargetRepository") {
            it("납부 대상 상태와 납부 상태를 집계한다") {
                val club = clubRepository.save(ClubTestFixture.createClub(code = "ACCOUNT-REPO-3"))
                val user1 = userRepository.save(UserTestFixture.createActiveUser1())
                val user2 = userRepository.save(UserTestFixture.createActiveUser2())
                val member1 =
                    clubMemberRepository.save(
                        ClubMemberTestFixture.createActiveMember(club = club, user = user1),
                    )
                val member2 =
                    clubMemberRepository.save(
                        ClubMemberTestFixture.createActiveMember(club = club, user = user2),
                    )
                val account = accountRepository.save(Account.createDraft(club = club, cardinal = 6))
                val paidTarget = AccountPaymentTarget.createTargeted(account, member1, Money.of(50_000))
                paidTarget.markPaid(
                    Money.of(50_000),
                    confirmedBy = 1L,
                    paidAt = LocalDate.of(2026, 3, 13).atStartOfDay(),
                )
                accountPaymentTargetRepository.save(paidTarget)
                accountPaymentTargetRepository.save(AccountPaymentTarget.createExcluded(account, member2))

                accountPaymentTargetRepository.countByAccountIdAndTargetStatus(
                    accountId = account.id,
                    targetStatus = AccountTargetStatus.TARGETED,
                ) shouldBe 1
                accountPaymentTargetRepository.countByAccountIdAndTargetStatusAndPaymentStatus(
                    accountId = account.id,
                    targetStatus = AccountTargetStatus.TARGETED,
                    paymentStatus = AccountPaymentStatus.PAID,
                ) shouldBe 1
            }

            it("활성 멤버의 납부 대상만 대상 수로 집계한다") {
                val club = clubRepository.save(ClubTestFixture.createClub(code = "ACCOUNT-REPO-ACTIVE-COUNT"))
                val account = accountRepository.save(Account.createDraft(club = club, cardinal = 6))
                val activeMember = createMember(club, "활성회원", "account-active-count-1@test.com")
                val bannedMember = createMember(club, "차단회원", "account-active-count-2@test.com", MemberStatus.BANNED)
                val excludedMember = createMember(club, "제외회원", "account-active-count-3@test.com")

                accountPaymentTargetRepository.save(
                    AccountPaymentTarget.createTargeted(account, activeMember, Money.of(50_000)),
                )
                accountPaymentTargetRepository.save(
                    AccountPaymentTarget.createTargeted(account, bannedMember, Money.of(50_000)),
                )
                accountPaymentTargetRepository.save(AccountPaymentTarget.createExcluded(account, excludedMember))

                accountPaymentTargetRepository.countActiveClubMemberTargetsByAccountIdAndTargetStatus(
                    accountId = account.id,
                    targetStatus = AccountTargetStatus.TARGETED,
                ) shouldBe 1
            }

            it("TARGETED 페이지 조회는 활성 멤버만 포함하고 키워드를 적용한다") {
                val club = clubRepository.save(ClubTestFixture.createClub(code = "ACCOUNT-REPO-TARGETED-PAGE"))
                val account = accountRepository.save(Account.createDraft(club = club, cardinal = 6))
                val matchedMember = createMember(club, "김활성", "account-targeted-page-1@test.com")
                val unmatchedMember = createMember(club, "박활성", "account-targeted-page-2@test.com")
                val bannedMember = createMember(club, "김차단", "account-targeted-page-3@test.com", MemberStatus.BANNED)

                accountPaymentTargetRepository.save(
                    AccountPaymentTarget.createTargeted(account, matchedMember, Money.of(50_000)),
                )
                accountPaymentTargetRepository.save(
                    AccountPaymentTarget.createTargeted(account, unmatchedMember, Money.of(50_000)),
                )
                accountPaymentTargetRepository.save(
                    AccountPaymentTarget.createTargeted(account, bannedMember, Money.of(50_000)),
                )

                val result =
                    accountPaymentTargetRepository.findAllActiveClubMemberTargetsByAccountIdAndTargetStatus(
                        accountId = account.id,
                        targetStatus = AccountTargetStatus.TARGETED,
                        keyword = "김",
                        pageable = PageRequest.of(0, 10),
                    )

                result.totalElements shouldBe 1
                result.content.map { it.clubMember.id } shouldContainExactly listOf(matchedMember.id)
            }

            it("등록 완료 정리 대상은 비활성 멤버의 TARGETED 미납 행만 조회한다") {
                val club = clubRepository.save(ClubTestFixture.createClub(code = "ACCOUNT-REPO-INACTIVE-UNPAID"))
                val account = accountRepository.save(Account.createDraft(club = club, cardinal = 6))
                val bannedUnpaidMember =
                    createMember(club, "미납차단", "account-inactive-unpaid-1@test.com", MemberStatus.BANNED)
                val bannedPaidMember =
                    createMember(club, "완납차단", "account-inactive-unpaid-2@test.com", MemberStatus.BANNED)
                val activeUnpaidMember = createMember(club, "미납활성", "account-inactive-unpaid-3@test.com")
                val bannedExcludedMember =
                    createMember(club, "제외차단", "account-inactive-unpaid-4@test.com", MemberStatus.BANNED)
                val cleanupTarget = AccountPaymentTarget.createTargeted(account, bannedUnpaidMember, Money.of(50_000))
                val paidTarget = AccountPaymentTarget.createTargeted(account, bannedPaidMember, Money.of(50_000))
                paidTarget.markPaid(Money.of(50_000), confirmedBy = 1L, paidAt = LocalDateTime.of(2026, 3, 1, 10, 0))

                accountPaymentTargetRepository.save(cleanupTarget)
                accountPaymentTargetRepository.save(paidTarget)
                accountPaymentTargetRepository.save(
                    AccountPaymentTarget.createTargeted(account, activeUnpaidMember, Money.of(50_000)),
                )
                accountPaymentTargetRepository.save(AccountPaymentTarget.createExcluded(account, bannedExcludedMember))

                val result =
                    accountPaymentTargetRepository.findAllUnpaidTargetsWithInactiveClubMemberByAccountId(
                        account.id,
                    )

                result.map { it.clubMember.id } shouldContainExactly listOf(bannedUnpaidMember.id)
            }

            it("기수별 제외 후보 조회는 해당 기수 활성 명부 중 TARGETED가 아닌 멤버만 반환한다") {
                val club = clubRepository.save(ClubTestFixture.createClub(code = "ACCOUNT-REPO-EXCLUDED-CANDIDATE"))
                val cardinal6 = cardinalRepository.save(Cardinal.create(club = club, cardinalNumber = 6))
                val cardinal7 = cardinalRepository.save(Cardinal.create(club = club, cardinalNumber = 7))
                val account = accountRepository.save(Account.createDraft(club = club, cardinal = 6))
                val targetedMember = createMember(club, "김대상", "account-excluded-candidate-1@test.com")
                val excludedMember = createMember(club, "김제외", "account-excluded-candidate-2@test.com")
                val noRowMember = createMember(club, "김행없음", "account-excluded-candidate-3@test.com")
                val otherCardinalMember = createMember(club, "김다른기수", "account-excluded-candidate-4@test.com")
                val bannedMember =
                    createMember(club, "김차단", "account-excluded-candidate-5@test.com", MemberStatus.BANNED)

                listOf(
                    targetedMember,
                    excludedMember,
                    noRowMember,
                    bannedMember,
                ).forEach { assignCardinal(it, cardinal6) }
                assignCardinal(otherCardinalMember, cardinal7)
                accountPaymentTargetRepository.save(
                    AccountPaymentTarget.createTargeted(account, targetedMember, Money.of(50_000)),
                )
                accountPaymentTargetRepository.save(AccountPaymentTarget.createExcluded(account, excludedMember))

                val result =
                    clubMemberRepository.findExcludedPaymentTargetCandidatesByCardinal(
                        clubId = club.id,
                        cardinalNumber = 6,
                        accountId = account.id,
                        keyword = "김",
                        pageable = PageRequest.of(0, 10),
                    )

                result.content.map { it.id } shouldContainExactly listOf(excludedMember.id, noRowMember.id)
            }
        }
    })
