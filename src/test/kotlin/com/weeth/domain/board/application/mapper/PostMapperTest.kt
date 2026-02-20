package com.weeth.domain.board.application.mapper

import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.comment.application.dto.response.CommentResponse
import com.weeth.domain.file.application.dto.response.FileResponse
import com.weeth.domain.file.domain.entity.FileStatus
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.enums.Role
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

class PostMapperTest :
    DescribeSpec({
        val mapper = PostMapper()
        val now = LocalDateTime.now()
        val user = mockk<User>()
        val post = mockk<Post>()

        every { user.name } returns "테스터"
        every { user.role } returns Role.USER

        every { post.id } returns 1L
        every { post.title } returns "제목"
        every { post.content } returns "내용"
        every { post.user } returns user
        every { post.commentCount } returns 2
        every { post.createdAt } returns now.minusHours(1)
        every { post.modifiedAt } returns now

        describe("toListResponse") {
            it("24시간 이내 생성된 게시글은 isNew=true") {
                val response = mapper.toListResponse(post, hasFile = true, now = now)

                response.id shouldBe 1L
                response.hasFile shouldBe true
                response.isNew shouldBe true
            }
        }

        describe("toDetailResponse") {
            it("댓글/파일 목록을 포함해 상세 응답으로 변환한다") {
                val comments =
                    listOf(
                        CommentResponse(
                            id = 10L,
                            name = "댓글작성자",
                            role = Role.USER,
                            content = "댓글",
                            time = LocalDateTime.now(),
                            fileUrls = emptyList(),
                            children = emptyList(),
                        ),
                    )
                val files =
                    listOf(
                        FileResponse(
                            fileId = 5L,
                            fileName = "a.png",
                            fileUrl = "https://cdn/a.png",
                            storageKey = "POST/2026-02/550e8400-e29b-41d4-a716-446655440000_a.png",
                            fileSize = 100,
                            contentType = "image/png",
                            status = FileStatus.UPLOADED,
                        ),
                    )

                val response = mapper.toDetailResponse(post, comments, files)

                response.id shouldBe 1L
                response.commentCount shouldBe 2
                response.comments.size shouldBe 1
                response.fileUrls.size shouldBe 1
            }
        }
    })
