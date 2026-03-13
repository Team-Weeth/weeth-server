package com.weeth.domain.board.application.usecase.command

import com.weeth.domain.board.application.exception.BoardNotInClubException
import com.weeth.domain.board.application.exception.BoardTypeMismatchException
import com.weeth.domain.board.domain.entity.LastNoticeRead
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.board.domain.repository.LastNoticeReadReader
import com.weeth.domain.board.domain.repository.LastNoticeReadRepository
import com.weeth.domain.board.fixture.BoardTestFixture
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.date.shouldBeAfter
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.util.ReflectionTestUtils

class MarkNoticeReadUseCaseTest :
    DescribeSpec({
        val boardRepository = mockk<BoardRepository>()
        val lastNoticeReadReader = mockk<LastNoticeReadReader>()
        val lastNoticeReadRepository = mockk<LastNoticeReadRepository>()
        val userReader = mockk<UserReader>()
        val clubMemberReader = mockk<ClubMemberReader>()
        val clubMemberPolicy = ClubMemberPolicy(clubMemberReader)

        val useCase =
            MarkNoticeReadUseCase(
                boardRepository = boardRepository,
                lastNoticeReadReader = lastNoticeReadReader,
                lastNoticeReadRepository = lastNoticeReadRepository,
                userReader = userReader,
                clubMemberPolicy = clubMemberPolicy,
            )

        beforeTest {
            clearMocks(boardRepository, lastNoticeReadReader, lastNoticeReadRepository, userReader, clubMemberReader)
        }

        describe("execute") {
            val userId = 1L
            val clubId = 1L
            val boardId = 1L
            val user = UserTestFixture.createActiveUser1(userId)
            val club = ClubTestFixture.createClub().also { ReflectionTestUtils.setField(it, "id", clubId) }
            val clubMember = ClubTestFixture.createClubMember(club = club, user = user)
            val noticeBoard = BoardTestFixture.createNoticeBoard(club = club)

            context("클럽 멤버가 아닌 경우") {
                it("ClubMemberNotFoundException을 던진다") {
                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns null

                    shouldThrow<com.weeth.domain.club.application.exception.ClubMemberNotFoundException> {
                        useCase.execute(userId, clubId, boardId)
                    }
                }
            }

            context("다른 클럽의 게시판인 경우") {
                it("BoardNotInClubException을 던진다") {
                    val otherClub = ClubTestFixture.createClub().also { ReflectionTestUtils.setField(it, "id", 99L) }
                    val boardInOtherClub = BoardTestFixture.createNoticeBoard(club = otherClub)

                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns clubMember
                    every { boardRepository.findByIdAndIsDeletedFalse(boardId) } returns boardInOtherClub

                    shouldThrow<BoardNotInClubException> {
                        useCase.execute(userId, clubId, boardId)
                    }
                }
            }

            context("공지 타입이 아닌 게시판인 경우") {
                it("BoardTypeMismatchException을 던진다") {
                    val generalBoard = BoardTestFixture.create(club = club)
                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns clubMember
                    every { boardRepository.findByIdAndIsDeletedFalse(boardId) } returns generalBoard

                    shouldThrow<BoardTypeMismatchException> {
                        useCase.execute(userId, clubId, boardId)
                    }
                }
            }

            context("이미 읽은 기록이 있는 경우") {
                it("lastReadAt을 현재 시각으로 갱신하고 새 레코드를 저장하지 않는다") {
                    val existing = LastNoticeRead.create(user = user, board = noticeBoard)
                    val beforeExecute = existing.lastReadAt
                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns clubMember
                    every { boardRepository.findByIdAndIsDeletedFalse(boardId) } returns noticeBoard
                    every { lastNoticeReadReader.findByUserIdAndBoardId(userId, boardId) } returns existing

                    useCase.execute(userId, clubId, boardId)

                    existing.lastReadAt shouldBeAfter beforeExecute
                    verify(exactly = 0) { userReader.getById(any()) }
                    verify(exactly = 0) { lastNoticeReadRepository.save(any()) }
                }
            }

            context("처음 읽는 경우") {
                it("새 LastNoticeRead 레코드를 저장한다") {
                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns clubMember
                    every { boardRepository.findByIdAndIsDeletedFalse(boardId) } returns noticeBoard
                    every { lastNoticeReadReader.findByUserIdAndBoardId(userId, boardId) } returns null
                    every { userReader.getById(userId) } returns user
                    every { lastNoticeReadRepository.save(any<LastNoticeRead>()) } answers { firstArg() }

                    useCase.execute(userId, clubId, boardId)

                    verify(exactly = 1) { userReader.getById(userId) }
                    verify(exactly = 1) { lastNoticeReadRepository.save(any<LastNoticeRead>()) }
                }
            }
        }
    })
