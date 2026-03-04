package com.weeth.domain.board.application.usecase.query

import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.mapper.BoardMapper
import com.weeth.domain.board.domain.entity.Board
import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.user.domain.enums.Role
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.repository.findByIdOrNull

class GetBoardQueryServiceTest :
    DescribeSpec({
        val boardRepository = mockk<BoardRepository>()
        val boardMapper = BoardMapper()
        val queryService = GetBoardQueryService(boardRepository, boardMapper)

        describe("findBoards") {
            it("일반 사용자에게는 공개 게시판만 반환한다") {
                val publicBoard = Board(name = "일반", type = BoardType.GENERAL)
                val privateBoard =
                    Board(name = "운영", type = BoardType.NOTICE).apply {
                        updateConfig(config.copy(isPrivate = true))
                    }

                every { boardRepository.findAllByIsDeletedFalseOrderByIdAsc() } returns
                    listOf(publicBoard, privateBoard)

                val result = queryService.findBoards(Role.USER)

                result shouldHaveSize 1
                result.first().name shouldBe "일반"
            }

            it("관리자에게는 비공개 게시판도 포함해 반환한다") {
                val publicBoard = Board(name = "일반", type = BoardType.GENERAL)
                val privateBoard =
                    Board(name = "운영", type = BoardType.NOTICE).apply {
                        updateConfig(config.copy(isPrivate = true))
                    }

                every { boardRepository.findAllByIsDeletedFalseOrderByIdAsc() } returns
                    listOf(publicBoard, privateBoard)

                val result = queryService.findBoards(Role.ADMIN)

                result shouldHaveSize 2
            }
        }

        describe("findAllBoardsForAdmin") {
            it("삭제된 게시판을 포함해 전체 목록을 반환한다") {
                val activeBoard = Board(name = "일반", type = BoardType.GENERAL)
                val deletedBoard =
                    Board(name = "삭제됨", type = BoardType.GENERAL).apply {
                        markDeleted()
                    }

                every { boardRepository.findAllByOrderByIdAsc() } returns listOf(activeBoard, deletedBoard)

                val result = queryService.findAllBoardsForAdmin()

                result shouldHaveSize 2
            }

            it("활성 게시판과 비공개 게시판도 모두 포함해 반환한다") {
                val publicBoard = Board(name = "일반", type = BoardType.GENERAL)
                val privateBoard =
                    Board(name = "운영", type = BoardType.NOTICE).apply {
                        updateConfig(config.copy(isPrivate = true))
                    }

                every { boardRepository.findAllByOrderByIdAsc() } returns listOf(publicBoard, privateBoard)

                val result = queryService.findAllBoardsForAdmin()

                result shouldHaveSize 2
            }
        }

        describe("findBoardDetailForAdmin") {
            context("관리자라면") {
                it("삭제된 게시판도 조회할 수 있다") {
                    val deletedBoard =
                        Board(name = "삭제됨", type = BoardType.GENERAL).apply {
                            markDeleted()
                        }
                    every { boardRepository.findByIdOrNull(3L) } returns deletedBoard

                    val result = queryService.findBoardDetailForAdmin(3L)

                    result.isDeleted shouldBe true
                }

                it("비공개 게시판도 조회할 수 있다") {
                    val privateBoard =
                        Board(name = "운영", type = BoardType.NOTICE).apply {
                            updateConfig(config.copy(isPrivate = true))
                        }
                    every { boardRepository.findByIdOrNull(2L) } returns privateBoard

                    val result = queryService.findBoardDetailForAdmin(2L)

                    result.isPrivate shouldBe true
                }

                it("존재하지 않는 boardId면 예외를 던진다") {
                    every { boardRepository.findByIdOrNull(999L) } returns null

                    shouldThrow<BoardNotFoundException> {
                        queryService.findBoardDetailForAdmin(999L)
                    }
                }
            }
        }
    })
