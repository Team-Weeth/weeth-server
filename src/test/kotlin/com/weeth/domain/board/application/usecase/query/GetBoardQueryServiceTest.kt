package com.weeth.domain.board.application.usecase.query

import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.mapper.BoardMapper
import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.repository.BoardPostCount
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.board.domain.repository.PostRepository
import com.weeth.domain.board.fixture.BoardTestFixture
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class GetBoardQueryServiceTest :
    DescribeSpec({
        val boardRepository = mockk<BoardRepository>()
        val postRepository = mockk<PostRepository>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>(relaxed = true)
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>(relaxed = true)
        val boardMapper = BoardMapper()
        val queryService =
            GetBoardQueryService(boardRepository, postRepository, clubMemberPolicy, clubPermissionPolicy, boardMapper)

        val clubId = 1L
        val userId = 10L

        beforeTest {
            clearMocks(boardRepository, postRepository, clubMemberPolicy, clubPermissionPolicy)
            every { postRepository.countActivePostsByBoardIds(any()) } returns emptyList()
        }

        describe("findBoards") {
            it("일반 사용자에게는 공개 게시판만 반환하고 전체 게시판은 항상 포함한다") {
                val noticeBoard = BoardTestFixture.create(name = "공지사항", type = BoardType.NOTICE)
                val publicBoard = BoardTestFixture.create(name = "일반", type = BoardType.GENERAL)
                val privateBoard =
                    BoardTestFixture.create(name = "운영", type = BoardType.GENERAL).apply {
                        updateConfig(config.copy(isPrivate = true))
                    }
                val member = ClubMemberTestFixture.createActiveMember()

                every { clubMemberPolicy.getActiveMember(clubId, userId) } returns member
                every { boardRepository.findAllByClubIdAndIsDeletedFalseOrderByDisplayOrderAscIdAsc(clubId) } returns
                    listOf(noticeBoard, publicBoard, privateBoard)

                val result = queryService.findBoards(clubId, userId)

                // 공지사항, 전체(가상), 일반 — 비공개 운영은 제외
                result shouldHaveSize 3
                result.map { it.name } shouldBe listOf("공지사항", "전체", "일반")
            }

            it("관리자에게는 비공개 게시판도 포함하고 순서는 공지사항 → 전체 → 나머지다") {
                val noticeBoard = BoardTestFixture.create(name = "공지사항", type = BoardType.NOTICE)
                val publicBoard = BoardTestFixture.create(name = "일반", type = BoardType.GENERAL)
                val privateBoard =
                    BoardTestFixture.create(name = "운영", type = BoardType.GENERAL).apply {
                        updateConfig(config.copy(isPrivate = true))
                    }
                val adminMember = ClubMemberTestFixture.createAdminMember()

                every { clubMemberPolicy.getActiveMember(clubId, userId) } returns adminMember
                every { boardRepository.findAllByClubIdAndIsDeletedFalseOrderByDisplayOrderAscIdAsc(clubId) } returns
                    listOf(noticeBoard, publicBoard, privateBoard)

                val result = queryService.findBoards(clubId, userId)

                result shouldHaveSize 4
                result.map { it.name } shouldBe listOf("공지사항", "전체", "일반", "운영")
            }

            it("전체 게시판은 항상 id가 null이고 type이 ALL이다") {
                val noticeBoard = BoardTestFixture.create(name = "공지사항", type = BoardType.NOTICE)
                val member = ClubMemberTestFixture.createActiveMember()

                every { clubMemberPolicy.getActiveMember(clubId, userId) } returns member
                every { boardRepository.findAllByClubIdAndIsDeletedFalseOrderByDisplayOrderAscIdAsc(clubId) } returns
                    listOf(noticeBoard)

                val result = queryService.findBoards(clubId, userId)

                val virtualAll = result.first { it.type == BoardType.ALL }
                virtualAll.id shouldBe null
                virtualAll.name shouldBe "전체"
            }
        }

        describe("findAllBoardsForAdmin") {
            it("삭제된 게시판을 포함해 전체 목록을 반환한다") {
                val activeBoard = BoardTestFixture.create(name = "일반", type = BoardType.GENERAL)
                val deletedBoard =
                    BoardTestFixture.create(name = "삭제됨", type = BoardType.GENERAL).apply {
                        markDeleted()
                    }

                every { boardRepository.findAllByClubIdOrderByDisplayOrderAscIdAsc(clubId) } returns
                    listOf(activeBoard, deletedBoard)

                val result = queryService.findAllBoardsForAdmin(clubId, userId)

                // 가상 전체 게시판 포함: 전체, 일반, 삭제됨
                result shouldHaveSize 3
                result.map { it.name } shouldBe listOf("전체", "일반", "삭제됨")
            }

            it("활성 게시판과 비공개 게시판도 모두 포함해 반환한다") {
                val publicBoard = BoardTestFixture.create(name = "일반", type = BoardType.GENERAL)
                val privateBoard =
                    BoardTestFixture.create(name = "운영", type = BoardType.NOTICE).apply {
                        updateConfig(config.copy(isPrivate = true))
                    }

                every { boardRepository.findAllByClubIdOrderByDisplayOrderAscIdAsc(clubId) } returns
                    listOf(publicBoard, privateBoard)

                val result = queryService.findAllBoardsForAdmin(clubId, userId)

                // NOTICE 타입인 운영 → noticeBoards 먼저, 가상 전체 다음, 나머지 순
                result shouldHaveSize 3
                result.map { it.name } shouldBe listOf("운영", "전체", "일반")
            }

            it("게시판별 활성 게시글 수를 포함해 반환한다") {
                val board = BoardTestFixture.create(id = 1L, name = "일반", type = BoardType.GENERAL)
                every { boardRepository.findAllByClubIdOrderByDisplayOrderAscIdAsc(clubId) } returns listOf(board)
                every { postRepository.countActivePostsByBoardIds(listOf(board.id)) } returns
                    listOf(BoardPostCount(boardId = board.id, postCount = 5L))

                val result = queryService.findAllBoardsForAdmin(clubId, userId)

                result.first().postCount shouldBe 5
            }

            it("게시판이 없으면 postRepository를 호출하지 않는다") {
                every { boardRepository.findAllByClubIdOrderByDisplayOrderAscIdAsc(clubId) } returns emptyList()

                queryService.findAllBoardsForAdmin(clubId, userId)

                verify(exactly = 0) { postRepository.countActivePostsByBoardIds(any()) }
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

            it("게시글 수가 postCount에 반영된다") {
                val board = BoardTestFixture.create(id = 1L, name = "일반", type = BoardType.GENERAL)
                every { boardRepository.findByIdAndClubId(board.id, clubId) } returns board
                every { postRepository.countActivePostsByBoardIds(listOf(board.id)) } returns
                    listOf(BoardPostCount(boardId = board.id, postCount = 3L))

                val result = queryService.findBoardDetailForAdmin(clubId, userId, board.id)

                result.postCount shouldBe 3
            }
        }

        describe("checkBoardNameDuplicate") {
            it("같은 클럽의 활성 게시판에 같은 이름이 있으면 중복으로 반환한다") {
                every { boardRepository.existsByClubIdAndNameAndIsDeletedFalse(clubId, "운영") } returns true

                val result = queryService.checkBoardNameDuplicate(clubId, userId, "운영")

                result.duplicated shouldBe true
            }

            it("수정 대상 boardId가 있으면 자기 자신은 중복 검사에서 제외한다") {
                every {
                    boardRepository.existsByClubIdAndNameAndIsDeletedFalseAndIdNot(clubId, "운영", 3L)
                } returns false

                val result = queryService.checkBoardNameDuplicate(clubId, userId, "운영", 3L)

                result.duplicated shouldBe false
            }
        }
    })
