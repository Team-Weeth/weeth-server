package com.weeth.domain.board.application.usecase.query

import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.mapper.BoardMapper
import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.board.fixture.BoardTestFixture
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.user.domain.enums.Role
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class GetBoardQueryServiceTest :
    DescribeSpec({
        val boardRepository = mockk<BoardRepository>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>(relaxed = true)
        val boardMapper = BoardMapper()
        val queryService = GetBoardQueryService(boardRepository, clubMemberPolicy, boardMapper)

        val clubId = 1L
        val userId = 10L

        describe("findBoards") {
            it("일반 사용자에게는 공개 게시판만 반환한다") {
                val publicBoard = BoardTestFixture.create(name = "일반", type = BoardType.GENERAL)
                val privateBoard =
                    BoardTestFixture.create(name = "운영", type = BoardType.NOTICE).apply {
                        updateConfig(config.copy(isPrivate = true))
                    }

                every { boardRepository.findAllByClubIdAndIsDeletedFalseOrderByIdAsc(clubId) } returns
                    listOf(publicBoard, privateBoard)

                val result = queryService.findBoards(clubId, userId, Role.USER)

                result shouldHaveSize 1
                result.first().name shouldBe "일반"
            }

            it("관리자에게는 비공개 게시판도 포함해 반환한다") {
                val publicBoard = BoardTestFixture.create(name = "일반", type = BoardType.GENERAL)
                val privateBoard =
                    BoardTestFixture.create(name = "운영", type = BoardType.NOTICE).apply {
                        updateConfig(config.copy(isPrivate = true))
                    }

                every { boardRepository.findAllByClubIdAndIsDeletedFalseOrderByIdAsc(clubId) } returns
                    listOf(publicBoard, privateBoard)

                val result = queryService.findBoards(clubId, userId, Role.ADMIN)

                result shouldHaveSize 2
                result.map { it.name } shouldBe listOf("일반", "운영")
            }
        }

        describe("findAllBoardsForAdmin") {
            it("삭제된 게시판을 포함해 전체 목록을 반환한다") {
                val activeBoard = BoardTestFixture.create(name = "일반", type = BoardType.GENERAL)
                val deletedBoard =
                    BoardTestFixture.create(name = "삭제됨", type = BoardType.GENERAL).apply {
                        markDeleted()
                    }

                every { boardRepository.findAllByClubIdOrderByIdAsc(clubId) } returns listOf(activeBoard, deletedBoard)

                val result = queryService.findAllBoardsForAdmin(clubId, userId)

                result shouldHaveSize 2
                result.map { it.name } shouldBe listOf("일반", "삭제됨")
            }

            it("활성 게시판과 비공개 게시판도 모두 포함해 반환한다") {
                val publicBoard = BoardTestFixture.create(name = "일반", type = BoardType.GENERAL)
                val privateBoard =
                    BoardTestFixture.create(name = "운영", type = BoardType.NOTICE).apply {
                        updateConfig(config.copy(isPrivate = true))
                    }

                every { boardRepository.findAllByClubIdOrderByIdAsc(clubId) } returns listOf(publicBoard, privateBoard)

                val result = queryService.findAllBoardsForAdmin(clubId, userId)

                result shouldHaveSize 2
                result.map { it.name } shouldBe listOf("일반", "운영")
            }
        }

        describe("findBoardDetailForAdmin") {
            it("삭제된 게시판도 조회할 수 있다") {
                val deletedBoard =
                    BoardTestFixture.create(name = "삭제됨", type = BoardType.GENERAL).apply {
                        markDeleted()
                    }
                every { boardRepository.findByIdAndClubId(3L, clubId) } returns deletedBoard

                val result = queryService.findBoardDetailForAdmin(clubId, userId, 3L)

                result.isDeleted shouldBe true
            }

            it("비공개 게시판도 조회할 수 있다") {
                val privateBoard =
                    BoardTestFixture.create(name = "운영", type = BoardType.NOTICE).apply {
                        updateConfig(config.copy(isPrivate = true))
                    }
                every { boardRepository.findByIdAndClubId(2L, clubId) } returns privateBoard

                val result = queryService.findBoardDetailForAdmin(clubId, userId, 2L)

                result.isPrivate shouldBe true
            }

            it("존재하지 않는 boardId면 예외를 던진다") {
                every { boardRepository.findByIdAndClubId(999L, clubId) } returns null

                shouldThrow<BoardNotFoundException> {
                    queryService.findBoardDetailForAdmin(clubId, userId, 999L)
                }
            }
        }
    })
