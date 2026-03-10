package com.weeth.domain.board.application.usecase.command

import com.weeth.domain.board.domain.entity.NoticeRead
import com.weeth.domain.board.domain.repository.NoticeReadReader
import com.weeth.domain.board.domain.repository.NoticeReadRepository
import com.weeth.domain.board.domain.repository.PostReader
import com.weeth.domain.board.fixture.BoardTestFixture
import com.weeth.domain.board.fixture.PostTestFixture
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class MarkNoticeReadUseCaseTest :
    DescribeSpec({
        val postReader = mockk<PostReader>()
        val noticeReadReader = mockk<NoticeReadReader>()
        val noticeReadRepository = mockk<NoticeReadRepository>()
        val userReader = mockk<UserReader>()

        val useCase =
            MarkNoticeReadUseCase(
                postReader = postReader,
                noticeReadReader = noticeReadReader,
                noticeReadRepository = noticeReadRepository,
                userReader = userReader,
            )

        beforeTest {
            clearMocks(postReader, noticeReadReader, noticeReadRepository, userReader)
        }

        describe("execute") {
            val userId = 1L
            val user = UserTestFixture.createActiveUser1(userId)
            val board = BoardTestFixture.create()
            val post = PostTestFixture.create(board = board)

            context("모든 공지를 이미 읽은 경우") {
                it("NoticeRead를 저장하지 않고 종료한다") {
                    every { postReader.findRecentByBoardTypeSince(any(), any()) } returns listOf(post)
                    every { noticeReadReader.findReadPostIdsByUserId(userId) } returns setOf(post.id)

                    useCase.execute(userId)

                    verify(exactly = 0) { userReader.getById(any()) }
                    verify(exactly = 0) { noticeReadRepository.saveAll(any<List<NoticeRead>>()) }
                }
            }

            context("읽지 않은 공지가 있는 경우") {
                it("미읽은 공지를 NoticeRead로 생성하고 일괄 저장한다") {
                    every { postReader.findRecentByBoardTypeSince(any(), any()) } returns listOf(post)
                    every { noticeReadReader.findReadPostIdsByUserId(userId) } returns emptySet()
                    every { userReader.getById(userId) } returns user
                    every { noticeReadRepository.saveAll(any<List<NoticeRead>>()) } answers { firstArg() }

                    useCase.execute(userId)

                    verify(exactly = 1) { userReader.getById(userId) }
                    verify(exactly = 1) { noticeReadRepository.saveAll(any<List<NoticeRead>>()) }
                }
            }

            context("2주 이내 공지가 없는 경우") {
                it("NoticeRead를 저장하지 않고 종료한다") {
                    every { postReader.findRecentByBoardTypeSince(any(), any()) } returns emptyList()
                    every { noticeReadReader.findReadPostIdsByUserId(userId) } returns emptySet()

                    useCase.execute(userId)

                    verify(exactly = 0) { userReader.getById(any()) }
                    verify(exactly = 0) { noticeReadRepository.saveAll(any<List<NoticeRead>>()) }
                }
            }
        }
    })
