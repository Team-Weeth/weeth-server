package com.weeth.domain.comment.application.usecase.query

import com.weeth.domain.board.domain.entity.enums.Category
import com.weeth.domain.board.fixture.PostTestFixture
import com.weeth.domain.comment.application.dto.response.CommentResponse
import com.weeth.domain.comment.application.mapper.CommentMapper
import com.weeth.domain.comment.fixture.CommentTestFixture
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.user.domain.entity.enums.Position
import com.weeth.domain.user.domain.entity.enums.Role
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
        val assembler = GetCommentQueryService(fileReader, fileMapper, commentMapper)

        val user = UserTestFixture.createActiveUser1(1L)
        val post = PostTestFixture.createPost(id = 10L, title = "title", category = Category.StudyLog)

        beforeTest {
            clearMocks(fileReader, fileMapper, commentMapper)
        }

        fun stubResponse(
            id: Long,
            children: List<CommentResponse> = emptyList(),
        ) = CommentResponse(
            id = id,
            name = "테스트유저",
            position = Position.BE,
            role = Role.USER,
            content = "content",
            time = LocalDateTime.now(),
            fileUrls = emptyList(),
            children = children,
        )

        describe("buildTree") {
            it("빈 리스트면 빈 리스트를 반환하고 파일 조회를 하지 않는다") {
                val result = assembler.toCommentTreeResponses(emptyList())

                result shouldBe emptyList()
                verify(exactly = 0) { fileReader.findAll(any(), any<Long>(), any()) }
                verify(exactly = 0) { fileReader.findAll(any(), any<List<Long>>(), any()) }
            }

            it("최상위 댓글만 있을 때 파일 조회를 1회 수행하고 트리를 반환한다") {
                val comment = CommentTestFixture.createPostComment(id = 1L, post = post, user = user)
                val response = stubResponse(1L)

                every { fileReader.findAll(FileOwnerType.COMMENT, listOf(1L), any()) } returns emptyList()
                every { commentMapper.toCommentDto(comment, emptyList(), emptyList()) } returns response

                val result = assembler.toCommentTreeResponses(listOf(comment))

                result.size shouldBe 1
                result[0].id shouldBe 1L
                verify(exactly = 1) { fileReader.findAll(FileOwnerType.COMMENT, listOf(1L), any()) }
            }

            it("부모-자식 구조가 있을 때 자식이 부모에 중첩된 트리로 조립된다") {
                val parent = CommentTestFixture.createPostComment(id = 10L, post = post, user = user)
                val child = CommentTestFixture.createPostComment(id = 11L, post = post, user = user, parent = parent)
                val childResponse = stubResponse(11L)
                val parentResponse = stubResponse(10L, children = listOf(childResponse))

                every { fileReader.findAll(FileOwnerType.COMMENT, listOf(10L, 11L), any()) } returns emptyList()
                every { commentMapper.toCommentDto(child, emptyList(), emptyList()) } returns childResponse
                every { commentMapper.toCommentDto(parent, listOf(childResponse), emptyList()) } returns parentResponse

                val result = assembler.toCommentTreeResponses(listOf(parent, child))

                result.size shouldBe 1
                result[0].id shouldBe 10L
                result[0].children.size shouldBe 1
                result[0].children[0].id shouldBe 11L
                verify(exactly = 1) { fileReader.findAll(FileOwnerType.COMMENT, listOf(10L, 11L), any()) }
            }

            it("자식 댓글은 최상위 목록에 포함되지 않는다") {
                val parent = CommentTestFixture.createPostComment(id = 10L, post = post, user = user)
                val child = CommentTestFixture.createPostComment(id = 11L, post = post, user = user, parent = parent)
                val childResponse = stubResponse(11L)
                val parentResponse = stubResponse(10L, children = listOf(childResponse))

                every { fileReader.findAll(FileOwnerType.COMMENT, listOf(10L, 11L), any()) } returns emptyList()
                every { commentMapper.toCommentDto(child, emptyList(), emptyList()) } returns childResponse
                every { commentMapper.toCommentDto(parent, listOf(childResponse), emptyList()) } returns parentResponse

                val result = assembler.toCommentTreeResponses(listOf(parent, child))

                // 최상위에는 parent만 있어야 함
                result.size shouldBe 1
                result.none { it.id == 11L } shouldBe true
            }
        }
    })
