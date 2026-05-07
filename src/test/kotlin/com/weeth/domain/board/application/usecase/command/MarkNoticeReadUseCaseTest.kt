package com.weeth.domain.board.application.usecase.command

import com.weeth.domain.board.application.exception.BoardNotInClubException
import com.weeth.domain.board.application.exception.BoardTypeMismatchException
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.board.domain.repository.LastNoticeReadRepository
import com.weeth.domain.board.fixture.BoardTestFixture
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.util.ReflectionTestUtils

class MarkNoticeReadUseCaseTest :
    DescribeSpec({
        val boardRepository = mockk<BoardRepository>()
        val lastNoticeReadRepository = mockk<LastNoticeReadRepository>()
        val clubMemberReader = mockk<ClubMemberReader>()
        val clubMemberPolicy = ClubMemberPolicy(clubMemberReader)

        val useCase =
            MarkNoticeReadUseCase(
                boardRepository = boardRepository,
                lastNoticeReadRepository = lastNoticeReadRepository,
                clubMemberPolicy = clubMemberPolicy,
            )

        beforeTest {
            clearMocks(boardRepository, lastNoticeReadRepository, clubMemberReader)
        }

        describe("execute") {
            val userId = 1L
            val clubId = 1L
            val boardId = 1L
            val user = UserTestFixture.createActiveUser1(1L)
            val club = ClubTestFixture.createClub().also { ReflectionTestUtils.setField(it, "id", clubId) }
            val clubMember =
                ClubTestFixture
                    .createClubMember(club = club, user = user)
                    .also { ReflectionTestUtils.setField(it, "id", 10L) }
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

            context("공지 게시판 읽음 처리 요청이 유효한 경우") {
                it("clubMember와 board 기준으로 마지막 읽음 시간을 기록한다") {
                    every { clubMemberReader.findByClubIdAndUserId(clubId, userId) } returns clubMember
                    every { boardRepository.findByIdAndIsDeletedFalse(boardId) } returns noticeBoard
                    every { lastNoticeReadRepository.markRead(clubMember.id, noticeBoard.id, any()) } returns 1

                    useCase.execute(userId, clubId, boardId)

                    verify(exactly = 1) { lastNoticeReadRepository.markRead(clubMember.id, noticeBoard.id, any()) }
                }
            }
        }
    })
