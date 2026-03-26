package com.weeth.domain.board.application.usecase.command

import com.weeth.domain.board.application.dto.request.CreateBoardRequest
import com.weeth.domain.board.application.dto.request.ReorderBoardsRequest
import com.weeth.domain.board.application.dto.request.UpdateBoardRequest
import com.weeth.domain.board.application.exception.BoardCreateLockTimeoutException
import com.weeth.domain.board.application.exception.BoardLimitExceededException
import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.exception.BoardNotInClubException
import com.weeth.domain.board.application.exception.DeletedBoardNotReorderableException
import com.weeth.domain.board.application.exception.DuplicateBoardIdException
import com.weeth.domain.board.application.exception.DuplicateBoardNameException
import com.weeth.domain.board.application.exception.FixedBoardNotDeletableException
import com.weeth.domain.board.application.exception.FixedBoardNotRenamableException
import com.weeth.domain.board.application.exception.FixedBoardNotReorderableException
import com.weeth.domain.board.application.mapper.BoardMapper
import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.board.domain.vo.BoardConfig
import com.weeth.domain.board.fixture.BoardTestFixture
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.dao.PessimisticLockingFailureException

class ManageBoardUseCaseTest :
    DescribeSpec({
        val boardRepository = mockk<BoardRepository>()
        val boardMapper = BoardMapper()
        val clubReader = mockk<ClubReader>()
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>(relaxed = true)
        val useCase = ManageBoardUseCase(boardRepository, boardMapper, clubReader, clubPermissionPolicy)

        val club = ClubTestFixture.createClub()
        val clubId = club.id
        val userId = 10L

        beforeTest {
            clearMocks(boardRepository, clubReader, clubPermissionPolicy)
            every { boardRepository.save(any()) } answers { firstArg() }
            every { clubReader.getClubByIdForUpdate(clubId) } returns club
            every { boardRepository.findMaxActiveDisplayOrderByClubId(clubId) } returns -1
            every { boardRepository.findMaxDisplayOrderByClubId(clubId) } returns -1
            every { boardRepository.existsByClubIdAndNameAndIsDeletedFalse(any(), any()) } returns false
            every { boardRepository.existsByClubIdAndNameAndIsDeletedFalseAndIdNot(any(), any(), any()) } returns false
            every { boardRepository.countByClubIdAndTypeNotAndIsDeletedFalse(any(), any()) } returns 0
        }

        describe("create") {
            it("요청값으로 게시판과 설정을 생성한다") {
                val request =
                    CreateBoardRequest(
                        name = "운영공지",
                        type = BoardType.NOTICE,
                        commentEnabled = false,
                        writePermission = MemberRole.ADMIN,
                        isPrivate = true,
                    )

                val result = useCase.create(clubId, request, userId)

                result.name shouldBe "운영공지"
                result.type shouldBe BoardType.NOTICE
                result.commentEnabled shouldBe false
                result.writePermission shouldBe MemberRole.ADMIN
                result.isPrivate shouldBe true
            }

            it("기존 게시판이 없으면 displayOrder 0으로 생성한다") {
                every { boardRepository.findMaxActiveDisplayOrderByClubId(clubId) } returns -1
                val request =
                    CreateBoardRequest(
                        name = "첫 게시판",
                        type = BoardType.GENERAL,
                        commentEnabled = true,
                        writePermission = MemberRole.USER,
                        isPrivate = false,
                    )

                val result = useCase.create(clubId, request, userId)

                result.displayOrder shouldBe 0
            }

            it("기존 게시판이 있으면 마지막 순서 다음으로 생성한다") {
                every { boardRepository.findMaxActiveDisplayOrderByClubId(clubId) } returns 2
                val request =
                    CreateBoardRequest(
                        name = "새 게시판",
                        type = BoardType.GENERAL,
                        commentEnabled = true,
                        writePermission = MemberRole.USER,
                        isPrivate = false,
                    )

                val result = useCase.create(clubId, request, userId)

                result.displayOrder shouldBe 3
            }

            it("Club 락 획득 타임아웃 시 BoardCreateLockTimeoutException을 던진다") {
                every { clubReader.getClubByIdForUpdate(clubId) } throws PessimisticLockingFailureException("")
                val request =
                    CreateBoardRequest(
                        name = "새 게시판",
                        type = BoardType.GENERAL,
                        commentEnabled = true,
                        writePermission = MemberRole.USER,
                        isPrivate = false,
                    )

                shouldThrow<BoardCreateLockTimeoutException> {
                    useCase.create(clubId, request, userId)
                }
            }

            it("게시판 수가 3개 이상이면 예외를 던진다") {
                every { boardRepository.countByClubIdAndTypeNotAndIsDeletedFalse(clubId, BoardType.NOTICE) } returns 3
                val request =
                    CreateBoardRequest(
                        name = "초과 게시판",
                        type = BoardType.GENERAL,
                        commentEnabled = true,
                        writePermission = MemberRole.USER,
                        isPrivate = false,
                    )

                shouldThrow<BoardLimitExceededException> {
                    useCase.create(clubId, request, userId)
                }
            }

            it("같은 클럽에 동일한 이름의 게시판이 이미 있으면 예외를 던진다") {
                every { boardRepository.existsByClubIdAndNameAndIsDeletedFalse(clubId, "중복 이름") } returns true
                val request =
                    CreateBoardRequest(
                        name = "중복 이름",
                        type = BoardType.GENERAL,
                        commentEnabled = true,
                        writePermission = MemberRole.USER,
                        isPrivate = false,
                    )

                shouldThrow<DuplicateBoardNameException> {
                    useCase.create(clubId, request, userId)
                }
            }
        }

        describe("update") {
            it("일부 필드만 전달되면 해당 필드만 갱신한다") {
                val board = BoardTestFixture.create(club = club, name = "기존", type = BoardType.GENERAL)
                every { boardRepository.findByIdAndIsDeletedFalse(1L) } returns board

                val result = useCase.update(clubId, 1L, UpdateBoardRequest(name = "변경", isPrivate = true), userId)

                result.name shouldBe "변경"
                result.commentEnabled shouldBe true
                result.writePermission shouldBe MemberRole.USER
                result.isPrivate shouldBe true
            }

            it("아무 필드도 전달되지 않으면 기존 값이 그대로 유지된다") {
                val board = BoardTestFixture.create(club = club, name = "기존", type = BoardType.GENERAL)
                every { boardRepository.findByIdAndIsDeletedFalse(1L) } returns board

                val result = useCase.update(clubId, 1L, UpdateBoardRequest(), userId)

                result.name shouldBe "기존"
                result.commentEnabled shouldBe true
                result.writePermission shouldBe MemberRole.USER
                result.isPrivate shouldBe false
            }

            it("존재하지 않는 게시판이면 예외를 던진다") {
                every { boardRepository.findByIdAndIsDeletedFalse(999L) } returns null

                shouldThrow<BoardNotFoundException> {
                    useCase.update(clubId, 999L, UpdateBoardRequest(name = "변경"), userId)
                }
            }

            it("변경할 이름이 같은 클럽의 다른 게시판 이름과 중복되면 예외를 던진다") {
                val board = BoardTestFixture.create(id = 1L, club = club, name = "기존", type = BoardType.GENERAL)
                every { boardRepository.findByIdAndIsDeletedFalse(1L) } returns board
                every { boardRepository.existsByClubIdAndNameAndIsDeletedFalseAndIdNot(clubId, "중복 이름", 1L) } returns
                    true

                shouldThrow<DuplicateBoardNameException> {
                    useCase.update(clubId, 1L, UpdateBoardRequest(name = "중복 이름"), userId)
                }
            }

            it("공지사항 게시판의 이름을 변경하면 예외를 던진다") {
                val noticeBoard = BoardTestFixture.create(id = 1L, club = club, name = "공지사항", type = BoardType.NOTICE)
                every { boardRepository.findByIdAndIsDeletedFalse(1L) } returns noticeBoard

                shouldThrow<FixedBoardNotRenamableException> {
                    useCase.update(clubId, 1L, UpdateBoardRequest(name = "새 이름"), userId)
                }
            }
        }

        describe("delete") {
            it("게시판을 soft delete 처리하고 displayOrder를 맨 아래로 이동한다") {
                val board = BoardTestFixture.create(club = club, name = "일반", type = BoardType.GENERAL)
                every { boardRepository.findByIdAndIsDeletedFalse(1L) } returns board
                every { boardRepository.findMaxDisplayOrderByClubId(clubId) } returns 2

                useCase.delete(clubId, 1L, userId)

                board.isDeleted shouldBe true
                board.displayOrder shouldBe 3
                verify(exactly = 0) { boardRepository.delete(any()) }
            }

            it("공지사항 게시판을 삭제하면 예외를 던진다") {
                val noticeBoard = BoardTestFixture.create(id = 1L, club = club, name = "공지사항", type = BoardType.NOTICE)
                every { boardRepository.findByIdAndIsDeletedFalse(1L) } returns noticeBoard

                shouldThrow<FixedBoardNotDeletableException> {
                    useCase.delete(clubId, 1L, userId)
                }
            }
        }

        describe("reorder") {
            it("요청 순서대로 displayOrder를 저장한다") {
                val board1 =
                    BoardTestFixture
                        .create(
                            id = 1L,
                            club = club,
                            name = "A",
                            type = BoardType.GENERAL,
                        ).also { it.reorder(0) }
                val board2 =
                    BoardTestFixture
                        .create(
                            id = 2L,
                            club = club,
                            name = "B",
                            type = BoardType.GENERAL,
                        ).also { it.reorder(1) }
                val board3 =
                    BoardTestFixture
                        .create(
                            id = 3L,
                            club = club,
                            name = "C",
                            type = BoardType.GENERAL,
                        ).also { it.reorder(2) }
                every {
                    boardRepository.findAllByClubIdOrderByDisplayOrderAscIdAsc(clubId)
                } returns listOf(board1, board2, board3)

                useCase.reorder(clubId, ReorderBoardsRequest(boardIds = listOf(2L, 3L, 1L)), userId)

                board2.displayOrder shouldBe 0
                board3.displayOrder shouldBe 1
                board1.displayOrder shouldBe 2
            }

            it("클럽 게시판 일부만 요청해도 해당 게시판끼리 순서를 교환한다") {
                val board1 =
                    BoardTestFixture
                        .create(
                            id = 1L,
                            club = club,
                            name = "A",
                            type = BoardType.GENERAL,
                        ).also { it.reorder(0) }
                val board2 =
                    BoardTestFixture
                        .create(
                            id = 2L,
                            club = club,
                            name = "B",
                            type = BoardType.GENERAL,
                        ).also { it.reorder(1) }
                val board3 =
                    BoardTestFixture
                        .create(
                            id = 3L,
                            club = club,
                            name = "C",
                            type = BoardType.GENERAL,
                        ).also { it.reorder(2) }
                every {
                    boardRepository.findAllByClubIdOrderByDisplayOrderAscIdAsc(clubId)
                } returns listOf(board1, board2, board3)

                // board1(0)과 board3(2)만 swap — board2는 변경 없음
                useCase.reorder(clubId, ReorderBoardsRequest(boardIds = listOf(3L, 1L)), userId)

                board3.displayOrder shouldBe 0
                board1.displayOrder shouldBe 2
                board2.displayOrder shouldBe 1
            }

            it("다른 클럽 게시판 ID가 포함되면 예외를 던진다") {
                val board1 = BoardTestFixture.create(id = 1L, club = club, name = "A", type = BoardType.GENERAL)
                val board2 = BoardTestFixture.create(id = 2L, club = club, name = "B", type = BoardType.GENERAL)
                // 클럽에 2개 게시판이 있는데 존재하지 않는 ID(99L) 요청
                every {
                    boardRepository.findAllByClubIdOrderByDisplayOrderAscIdAsc(clubId)
                } returns listOf(board1, board2)

                shouldThrow<BoardNotInClubException> {
                    useCase.reorder(clubId, ReorderBoardsRequest(boardIds = listOf(1L, 99L)), userId)
                }
            }

            it("중복된 boardId가 포함되면 예외를 던진다") {
                // DB 조회 전에 중복 체크로 예외 발생 → repository 호출 없음
                shouldThrow<DuplicateBoardIdException> {
                    useCase.reorder(clubId, ReorderBoardsRequest(boardIds = listOf(1L, 1L, 2L)), userId)
                }
                verify(exactly = 0) {
                    boardRepository.findAllByClubIdOrderByDisplayOrderAscIdAsc(any())
                }
            }

            it("공지사항 ID를 요청에 포함하면 예외를 던진다") {
                val noticeBoard = BoardTestFixture.create(id = 1L, club = club, name = "공지사항", type = BoardType.NOTICE)
                val board2 = BoardTestFixture.create(id = 2L, club = club, name = "B", type = BoardType.GENERAL)
                every {
                    boardRepository.findAllByClubIdOrderByDisplayOrderAscIdAsc(clubId)
                } returns listOf(noticeBoard, board2)

                shouldThrow<FixedBoardNotReorderableException> {
                    useCase.reorder(clubId, ReorderBoardsRequest(boardIds = listOf(1L, 2L)), userId)
                }
            }

            it("삭제된 게시판 ID를 요청에 포함하면 예외를 던진다") {
                val board1 = BoardTestFixture.create(id = 1L, club = club, name = "A", type = BoardType.GENERAL)
                val deletedBoard =
                    BoardTestFixture
                        .create(
                            id = 2L,
                            club = club,
                            name = "B",
                            type = BoardType.GENERAL,
                        ).also {
                            it.markDeleted()
                        }
                every {
                    boardRepository.findAllByClubIdOrderByDisplayOrderAscIdAsc(clubId)
                } returns listOf(board1, deletedBoard)

                shouldThrow<DeletedBoardNotReorderableException> {
                    useCase.reorder(clubId, ReorderBoardsRequest(boardIds = listOf(1L, 2L)), userId)
                }
            }

            it("요청한 게시판끼리 슬롯을 교환하고 나머지는 그대로 유지한다") {
                val noticeBoard = BoardTestFixture.create(id = 1L, club = club, name = "공지사항", type = BoardType.NOTICE)
                val board2 =
                    BoardTestFixture
                        .create(
                            id = 2L,
                            club = club,
                            name = "B",
                            type = BoardType.GENERAL,
                        ).also { it.reorder(0) }
                val board3 =
                    BoardTestFixture
                        .create(
                            id = 3L,
                            club = club,
                            name = "C",
                            type = BoardType.GENERAL,
                        ).also { it.reorder(1) }
                val board4 =
                    BoardTestFixture
                        .create(
                            id = 4L,
                            club = club,
                            name = "D",
                            type = BoardType.GENERAL,
                        ).also { it.reorder(2) }
                every {
                    boardRepository.findAllByClubIdOrderByDisplayOrderAscIdAsc(clubId)
                } returns listOf(noticeBoard, board2, board3, board4)

                // board3(1)과 board2(0)를 swap — board4는 변경 없음
                useCase.reorder(clubId, ReorderBoardsRequest(boardIds = listOf(3L, 2L)), userId)

                board3.displayOrder shouldBe 0
                board2.displayOrder shouldBe 1
                board4.displayOrder shouldBe 2
            }
        }
    })
