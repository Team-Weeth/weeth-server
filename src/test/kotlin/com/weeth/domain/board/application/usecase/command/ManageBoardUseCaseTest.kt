package com.weeth.domain.board.application.usecase.command

import com.weeth.domain.board.application.dto.request.CreateBoardRequest
import com.weeth.domain.board.application.dto.request.UpdateBoardRequest
import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.mapper.BoardMapper
import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.board.domain.vo.BoardConfig
import com.weeth.domain.board.fixture.BoardTestFixture
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class ManageBoardUseCaseTest :
    DescribeSpec({
        val boardRepository = mockk<BoardRepository>()
        val boardMapper = BoardMapper()
        val clubReader = mockk<ClubReader>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>(relaxed = true)
        val useCase = ManageBoardUseCase(boardRepository, boardMapper, clubReader, clubMemberPolicy)

        val club = ClubTestFixture.createClub()
        val clubId = club.id
        val userId = 10L

        beforeTest {
            clearMocks(boardRepository, clubReader, clubMemberPolicy)
            every { boardRepository.save(any()) } answers { firstArg() }
            every { clubReader.getClubById(clubId) } returns club
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
        }

        describe("delete") {
            it("게시판을 soft delete 처리한다") {
                val board = BoardTestFixture.create(club = club, name = "일반", type = BoardType.GENERAL)
                every { boardRepository.findByIdAndIsDeletedFalse(1L) } returns board

                useCase.delete(clubId, 1L, userId)

                board.isDeleted shouldBe true
                verify(exactly = 0) { boardRepository.delete(any()) }
            }
        }
    })
