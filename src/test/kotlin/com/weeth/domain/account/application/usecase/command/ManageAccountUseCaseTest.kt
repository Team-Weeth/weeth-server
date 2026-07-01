package com.weeth.domain.account.application.usecase.command

import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.domain.entity.Account
import com.weeth.domain.account.domain.enums.AccountStatus
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.account.domain.vo.Money
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class ManageAccountUseCaseTest :
    DescribeSpec({
        val accountRepository = mockk<AccountRepository>(relaxed = true)
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>(relaxed = true)
        val useCase =
            ManageAccountUseCase(
                accountRepository = accountRepository,
                clubPermissionPolicy = clubPermissionPolicy,
            )

        val clubId = 1L
        val userId = 100L
        val club = ClubTestFixture.createClub(id = clubId)

        beforeTest {
            clearMocks(accountRepository, clubPermissionPolicy)
        }

        describe("updateMemberVisibility") {
            it("부원 거래 내역 공개 여부와 마지막 수정자를 저장한다") {
                val account = Account.createDraft(club = club, cardinal = 5)
                account.updateBasicInfo("5기 회비", Money.of(30_000), "운영비")
                account.activate()
                every { accountRepository.findByIdWithLock(1L) } returns account

                useCase.updateMemberVisibility(clubId = clubId, accountId = 1L, visible = true, userId = userId)

                account.status shouldBe AccountStatus.ACTIVE
                account.memberVisible shouldBe true
                account.lastModifiedBy shouldBe userId
            }

            it("다른 동아리 장부이면 AccountNotFoundException을 던지고 공개 상태를 바꾸지 않는다") {
                val otherClub = ClubTestFixture.createClub(id = 2L, code = "OTHER-CLUB")
                val account = Account.createDraft(club = otherClub, cardinal = 5)
                account.updateBasicInfo("5기 회비", Money.of(30_000), "운영비")
                account.activate()
                every { accountRepository.findByIdWithLock(1L) } returns account

                shouldThrow<AccountNotFoundException> {
                    useCase.updateMemberVisibility(clubId = clubId, accountId = 1L, visible = true, userId = userId)
                }

                account.memberVisible shouldBe false
                account.lastModifiedBy shouldBe null
            }
        }
    })
