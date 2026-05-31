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
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
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
    private val userRepository: UserRepository,
) : DescribeSpec({
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
        }
    })
