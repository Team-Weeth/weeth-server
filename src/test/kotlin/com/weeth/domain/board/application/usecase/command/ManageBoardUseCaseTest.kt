package com.weeth.domain.board.application.usecase.command

import com.weeth.domain.board.application.dto.request.CreateBoardRequest
import com.weeth.domain.board.application.dto.request.UpdateBoardRequest
import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.mapper.BoardMapper
import com.weeth.domain.board.domain.entity.Board
import com.weeth.domain.board.domain.entity.enums.BoardType
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.user.domain.entity.enums.Role
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class ManageBoardUseCaseTest :
    DescribeSpec({
        val boardRepository = mockk<BoardRepository>()
        val boardMapper = BoardMapper()
        val useCase = ManageBoardUseCase(boardRepository, boardMapper)

        beforeTest {
            every { boardRepository.save(any()) } answers { firstArg() }
        }

        describe("create") {
            it("요청값으로 게시판과 설정을 생성한다") {
                val request =
                    CreateBoardRequest(
                        name = "운영공지",
                        type = BoardType.NOTICE,
                        commentEnabled = false,
                        writePermission = Role.ADMIN,
                        isPrivate = true,
                    )

                val result = useCase.create(request)

                result.name shouldBe "운영공지"
                result.type shouldBe BoardType.NOTICE
                result.commentEnabled shouldBe false
                result.writePermission shouldBe Role.ADMIN
                result.isPrivate shouldBe true
            }
        }

        describe("update") {
            it("일부 필드만 전달되면 해당 필드만 갱신한다") {
                val board = Board(id = 1L, name = "기존", type = BoardType.GENERAL)
                every { boardRepository.findByIdAndIsDeletedFalse(1L) } returns board

                val result = useCase.update(1L, UpdateBoardRequest(name = "변경", isPrivate = true))

                result.name shouldBe "변경"
                result.commentEnabled shouldBe true
                result.writePermission shouldBe Role.USER
                result.isPrivate shouldBe true
            }

            it("존재하지 않는 게시판이면 예외를 던진다") {
                every { boardRepository.findByIdAndIsDeletedFalse(999L) } returns null

                shouldThrow<BoardNotFoundException> {
                    useCase.update(999L, UpdateBoardRequest(name = "변경"))
                }
            }
        }

        describe("delete") {
            it("게시판을 soft delete 처리한다") {
                val board = Board(id = 1L, name = "일반", type = BoardType.GENERAL)
                every { boardRepository.findByIdAndIsDeletedFalse(1L) } returns board

                useCase.delete(1L)

                board.isDeleted shouldBe true
                verify(exactly = 0) { boardRepository.delete(any()) }
            }
        }
    })
