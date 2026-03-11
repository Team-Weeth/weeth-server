package com.weeth.domain.account.application.usecase.command

import com.weeth.domain.account.application.dto.request.AccountSaveRequest
import com.weeth.domain.account.application.exception.AccountExistsException
import com.weeth.domain.account.domain.repository.AccountRepository
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class ManageAccountUseCaseTest :
    DescribeSpec({
        val accountRepository = mockk<AccountRepository>(relaxed = true)
        val cardinalReader = mockk<CardinalReader>(relaxed = true)
        val clubReader = mockk<ClubReader>(relaxed = true)
        val clubMemberPolicy = mockk<ClubMemberPolicy>(relaxed = true)
        val useCase = ManageAccountUseCase(accountRepository, cardinalReader, clubReader, clubMemberPolicy)

        val clubId = 1L
        val userId = 100L
        val club = ClubTestFixture.createClub()

        beforeTest {
            clearMocks(accountRepository, cardinalReader, clubReader, clubMemberPolicy)
            every { clubReader.getClubById(clubId) } returns club
        }

        describe("save") {
            context("이미 존재하는 기수로 저장 시") {
                it("AccountExistsException을 던진다") {
                    val request = AccountSaveRequest("설명", 100_000, 40)
                    every { accountRepository.existsByClubIdAndCardinal(clubId, 40) } returns true

                    shouldThrow<AccountExistsException> { useCase.save(clubId, request, userId) }
                }
            }

            context("정상 저장 시") {
                it("기수 존재를 보장하고 account를 저장한다") {
                    val request = AccountSaveRequest("설명", 100_000, 40)
                    every { accountRepository.existsByClubIdAndCardinal(clubId, 40) } returns false
                    every { cardinalReader.findByClubIdAndCardinalNumber(clubId, 40) } returns
                        CardinalTestFixture.createCardinal(cardinalNumber = 40, year = 2026, semester = 1)
                    every { accountRepository.save(any()) } answers { firstArg() }

                    useCase.save(clubId, request, userId)

                    verify(exactly = 1) { accountRepository.save(any()) }
                }
            }
        }
    })
