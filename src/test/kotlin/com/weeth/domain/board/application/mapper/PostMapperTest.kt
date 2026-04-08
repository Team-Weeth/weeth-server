package com.weeth.domain.board.application.mapper

import com.weeth.domain.board.domain.entity.Board
import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.comment.application.dto.response.CommentResponse
import com.weeth.domain.file.application.dto.response.FileResponse
import com.weeth.domain.file.domain.enums.FileStatus
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.application.dto.response.UserInfo
import com.weeth.domain.user.domain.entity.User
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import kotlin.collections.emptyList

class PostMapperTest :
    DescribeSpec({
        val fileAccessUrlPort = mockk<FileAccessUrlPort>()
        val mapper = PostMapper(fileAccessUrlPort)
        val now = LocalDateTime.now()
        val user = mockk<User>()
        val board = mockk<Board>()
        val post = mockk<Post>()
        val authorMember = mockk<ClubMember>()

        every { user.id } returns 1L
        every { user.name } returns "테스터"

        every { board.id } returns 10L
        every { board.name } returns "일반 게시판"

        every { authorMember.memberRole } returns MemberRole.USER
        every { authorMember.profileImageStorageKey } returns null
        every { authorMember.user } returns user

        every { post.id } returns 1L
        every { post.title } returns "제목"
        every { post.content } returns "내용"
        every { post.clubMember } returns authorMember
        every { post.board } returns board
        every { post.commentCount } returns 2
        every { post.likeCount } returns 0
        every { post.createdAt } returns now.minusHours(1)
        every { post.modifiedAt } returns now

        describe("toListResponse") {
            it("24시간 이내 생성된 게시글은 isNew=true") {
                val response =
                    mapper.toListResponse(
                        post,
                        files = emptyList(),
                        now = now,
                        isLiked = false,
                    )

                response.id shouldBe 1L
                response.fileUrls shouldBe emptyList()
                response.isNew shouldBe true
            }
        }

        describe("toDetailResponse") {
            it("댓글/파일 목록을 포함해 상세 응답으로 변환한다") {
                val comments =
                    listOf(
                        CommentResponse(
                            id = 10L,
                            author = UserInfo(id = 2L, name = "댓글작성자", profileImageUrl = null, role = MemberRole.USER),
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

                val response = mapper.toDetailResponse(post, comments, files, isLiked = false, now = now)

                response.id shouldBe 1L
                response.commentCount shouldBe 2
                response.comments.size shouldBe 1
                response.fileUrls.size shouldBe 1
            }
        }
    })
