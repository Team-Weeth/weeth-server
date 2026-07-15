package com.weeth.domain.comment.application.usecase.query

import com.weeth.domain.board.fixture.PostTestFixture
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.comment.application.dto.response.CommentResponse
import com.weeth.domain.comment.application.mapper.CommentMapper
import com.weeth.domain.comment.fixture.CommentTestFixture
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.enums.FileStatus
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.user.application.dto.response.UserInfo
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class GetCommentQueryServiceTest :
    DescribeSpec({
        val fileReader = mockk<FileReader>()
        val fileMapper = mockk<FileMapper>()
        val commentMapper = mockk<CommentMapper>()
        val service = GetCommentQueryService(fileReader, fileMapper, commentMapper)

        val user = UserTestFixture.createActiveUser1(1L)
        val member = ClubMemberTestFixture.createActiveMember(user = user)
        val post = PostTestFixture.create(clubMember = member)

        beforeTest {
            clearMocks(fileReader, fileMapper, commentMapper)
        }

        fun stubResponse(
            id: Long,
            children: List<CommentResponse> = emptyList(),
        ) = CommentResponse(
            id = id,
            author = UserInfo(id = 1L, name = "테스트유저", profileImageUrl = null, role = MemberRole.USER),
            content = "content",
            time = LocalDateTime.now(),
            fileUrls = emptyList(),
            children = children,
        )

        describe("toCommentTreeResponses") {
            it("빈 리스트면 빈 리스트를 반환하고 파일 조회를 하지 않는다") {
                val result = service.toCommentTreeResponses(emptyList())

                result shouldBe emptyList()
                verify(exactly = 0) { fileReader.findAll(any(), any<Long>(), any()) }
                verify(exactly = 0) { fileReader.findAll(any(), any<List<Long>>(), any()) }
            }

            it("최상위 댓글만 있을 때 파일 조회를 1회 수행한다") {
                val comment = CommentTestFixture.createPostComment(id = 1L, post = post, clubMember = member)
                val response = stubResponse(1L)

                every { fileReader.findAll(FileOwnerType.COMMENT, listOf(1L), FileStatus.UPLOADED) } returns emptyList()
                every { commentMapper.toCommentDto(comment, emptyList(), emptyList()) } returns response

                val result = service.toCommentTreeResponses(listOf(comment))

                result.size shouldBe 1
                result[0].id shouldBe 1L
                verify(exactly = 1) { fileReader.findAll(FileOwnerType.COMMENT, listOf(1L), FileStatus.UPLOADED) }
            }

            it("부모-자식 구조를 트리로 조립한다") {
                val parent = CommentTestFixture.createPostComment(id = 10L, post = post, clubMember = member)
                val child =
                    CommentTestFixture.createPostComment(
                        id = 11L,
                        post = post,
                        clubMember = member,
                        parent = parent,
                    )
                val childResponse = stubResponse(11L)
                val parentResponse = stubResponse(10L, children = listOf(childResponse))

                every { fileReader.findAll(FileOwnerType.COMMENT, listOf(10L, 11L), FileStatus.UPLOADED) } returns
                    emptyList()
                every { commentMapper.toCommentDto(child, emptyList(), emptyList()) } returns childResponse
                every { commentMapper.toCommentDto(parent, listOf(childResponse), emptyList()) } returns parentResponse

                val result = service.toCommentTreeResponses(listOf(parent, child))

                result.size shouldBe 1
                result[0].id shouldBe 10L
                result[0].children.size shouldBe 1
                result[0].children[0].id shouldBe 11L
            }
        }
    })
