package com.weeth.domain.board.application.mapper

import com.weeth.domain.board.domain.entity.Board
import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.comment.application.dto.response.CommentResponse
import com.weeth.domain.file.application.dto.response.FileResponse
import com.weeth.domain.file.domain.enums.FileStatus
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.application.dto.response.UserInfo
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.UserProfile
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.test.util.ReflectionTestUtils
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
        every { board.canWriteBy(any()) } returns true
        every { board.isCommentEnabled } returns true

        every { authorMember.memberRole } returns MemberRole.USER
        every { authorMember.memberStatus } returns MemberStatus.ACTIVE
        every { authorMember.profileImageStorageKey } returns null
        every { authorMember.userProfile } returns null
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
                        memberRole = MemberRole.USER,
                    )

                response.id shouldBe 1L
                response.fileUrls shouldBe emptyList()
                response.isNew shouldBe true
            }

            it("작성자 정보는 멤버가 사용 중인 멀티프로필 기준으로 변환한다") {
                val profile =
                    UserProfile
                        .create(
                            user = user,
                            name = "멀티프로필",
                            profileImageStorageKey = "USER_PROFILE_IMAGE/2026-07/profile.png",
                        ).apply {
                            ReflectionTestUtils.setField(this, "id", 10L)
                        }
                every { authorMember.userProfile } returns profile
                every { authorMember.profileImageStorageKey } returns "CLUB_MEMBER_PROFILE/legacy.png"
                every { fileAccessUrlPort.resolve("CLUB_MEMBER_PROFILE/legacy.png") } returns
                    "https://cdn.test/legacy.png"
                every { fileAccessUrlPort.resolve("USER_PROFILE_IMAGE/2026-07/profile.png") } returns
                    "https://cdn.test/profile.png"

                val response =
                    mapper.toListResponse(
                        post,
                        files = emptyList(),
                        now = now,
                        isLiked = false,
                        memberRole = MemberRole.USER,
                    )

                response.author.name shouldBe "멀티프로필"
                response.author.profileImageUrl shouldBe "https://cdn.test/profile.png"
            }
        }

        describe("작성자 익명화") {
            it("LEFT 멤버가 작성한 게시글은 작성자 이름이 익명 라벨로 치환되고 프로필이 null이 된다") {
                every { fileAccessUrlPort.resolve("POST/2026-05/leak.png") } returns "https://cdn/leak.png"
                val leftUser = mockk<User>()
                every { leftUser.id } returns 7L
                every { leftUser.name } returns "노출되면안됨"

                val leftMember = mockk<ClubMember>()
                every { leftMember.memberRole } returns MemberRole.USER
                every { leftMember.memberStatus } returns MemberStatus.LEFT
                every { leftMember.profileImageStorageKey } returns "POST/2026-05/leak.png"
                every { leftMember.userProfile } returns null
                every { leftMember.user } returns leftUser

                val leftPost = mockk<Post>()
                every { leftPost.id } returns 100L
                every { leftPost.title } returns "탈퇴자 글"
                every { leftPost.content } returns "내용"
                every { leftPost.clubMember } returns leftMember
                every { leftPost.board } returns board
                every { leftPost.commentCount } returns 0
                every { leftPost.likeCount } returns 0
                every { leftPost.createdAt } returns now
                every { leftPost.modifiedAt } returns now

                val response =
                    mapper.toListResponse(
                        leftPost,
                        files = emptyList(),
                        now = now,
                        isLiked = false,
                        memberRole = MemberRole.USER,
                    )

                response.author.name shouldBe UserInfo.ANONYMOUS_USER_NAME
                response.author.profileImageUrl shouldBe null
                response.author.id shouldBe 7L
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

                val response =
                    mapper.toDetailResponse(
                        post,
                        comments,
                        files,
                        isLiked = false,
                        now = now,
                        memberRole = MemberRole.USER,
                    )

                response.id shouldBe 1L
                response.commentCount shouldBe 2
                response.comments.size shouldBe 1
                response.fileUrls.size shouldBe 1
            }
        }
    })
