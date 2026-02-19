package com.weeth.domain.board.application.usecase.query

import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.mapper.BoardMapper
import com.weeth.domain.board.domain.entity.Board
import com.weeth.domain.board.domain.entity.enums.BoardType
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.user.domain.entity.enums.Role
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class GetBoardQueryServiceTest :
    DescribeSpec({
        val boardRepository = mockk<BoardRepository>()
        val boardMapper = BoardMapper()
        val queryService = GetBoardQueryService(boardRepository, boardMapper)

        describe("findBoards") {
            it("일반 사용자에게는 공개 게시판만 반환한다") {
                val publicBoard = Board(id = 1L, name = "일반", type = BoardType.GENERAL)
                val privateBoard =
                    Board(id = 2L, name = "운영", type = BoardType.NOTICE).apply {
                        updateConfig(config.copy(isPrivate = true))
                    }

                every { boardRepository.findAllByIsDeletedFalseOrderByIdAsc() } returns listOf(publicBoard, privateBoard)

                val result = queryService.findBoards(Role.USER)

                result shouldHaveSize 1
                result.first().id shouldBe 1L
            }

            it("관리자에게는 비공개 게시판도 포함해 반환한다") {
                val publicBoard = Board(id = 1L, name = "일반", type = BoardType.GENERAL)
                val privateBoard =
                    Board(id = 2L, name = "운영", type = BoardType.NOTICE).apply {
                        updateConfig(config.copy(isPrivate = true))
                    }

                every { boardRepository.findAllByIsDeletedFalseOrderByIdAsc() } returns listOf(publicBoard, privateBoard)

                val result = queryService.findBoards(Role.ADMIN)

                result shouldHaveSize 2
            }
        }

        describe("findBoard") {
            it("일반 사용자가 비공개 게시판 상세를 조회하면 예외를 던진다") {
                val privateBoard =
                    Board(id = 2L, name = "운영", type = BoardType.NOTICE).apply {
                        updateConfig(config.copy(isPrivate = true))
                    }
                every { boardRepository.findByIdAndIsDeletedFalse(2L) } returns privateBoard

                shouldThrow<BoardNotFoundException> {
                    queryService.findBoard(2L, Role.USER)
                }
            }

            it("관리자는 비공개 게시판 상세를 조회할 수 있다") {
                val privateBoard =
                    Board(id = 2L, name = "운영", type = BoardType.NOTICE).apply {
                        updateConfig(config.copy(isPrivate = true))
                    }
                every { boardRepository.findByIdAndIsDeletedFalse(2L) } returns privateBoard

                val result = queryService.findBoard(2L, Role.ADMIN)

                result.id shouldBe 2L
                result.isPrivate shouldBe true
            }
        }
    })
