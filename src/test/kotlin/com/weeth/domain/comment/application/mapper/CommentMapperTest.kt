package com.weeth.domain.comment.application.mapper

import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.comment.domain.entity.Comment
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.application.dto.response.UserInfo
import com.weeth.domain.user.domain.entity.User
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

class CommentMapperTest :
    DescribeSpec({
        val fileAccessUrlPort = mockk<FileAccessUrlPort>()
        val mapper = CommentMapper(fileAccessUrlPort)
        val now = LocalDateTime.now()

        describe("toCommentDto 작성자 익명화") {
            it("ACTIVE 멤버 댓글은 실제 작성자 정보가 노출된다") {
                every { fileAccessUrlPort.resolve("CLUB_MEMBER_PROFILE/active.png") } returns "https://cdn/active.png"
                val activeUser = mockk<User>()
                every { activeUser.id } returns 1L
                every { activeUser.name } returns "활동중"

                val activeMember = mockk<ClubMember>()
                every { activeMember.memberRole } returns MemberRole.USER
                every { activeMember.memberStatus } returns MemberStatus.ACTIVE
                every { activeMember.profileImageStorageKey } returns "CLUB_MEMBER_PROFILE/active.png"
                every { activeMember.user } returns activeUser

                val comment = mockk<Comment>()
                every { comment.id } returns 10L
                every { comment.clubMember } returns activeMember
                every { comment.content } returns "정상 댓글"
                every { comment.createdAt } returns now

                val response = mapper.toCommentDto(comment, children = emptyList(), fileUrls = emptyList())

                response.author.name shouldBe "활동중"
                response.author.profileImageUrl shouldBe "https://cdn/active.png"
            }

            it("LEFT 멤버 댓글은 작성자 이름이 익명 라벨로 치환되고 프로필이 null이 된다") {
                every { fileAccessUrlPort.resolve("CLUB_MEMBER_PROFILE/leak.png") } returns "https://cdn/leak.png"
                val leftUser = mockk<User>()
                every { leftUser.id } returns 2L
                every { leftUser.name } returns "노출되면안됨"

                val leftMember = mockk<ClubMember>()
                every { leftMember.memberRole } returns MemberRole.USER
                every { leftMember.memberStatus } returns MemberStatus.LEFT
                every { leftMember.profileImageStorageKey } returns "CLUB_MEMBER_PROFILE/leak.png"
                every { leftMember.user } returns leftUser

                val comment = mockk<Comment>()
                every { comment.id } returns 20L
                every { comment.clubMember } returns leftMember
                every { comment.content } returns "탈퇴자 댓글"
                every { comment.createdAt } returns now

                val response = mapper.toCommentDto(comment, children = emptyList(), fileUrls = emptyList())

                response.author.name shouldBe UserInfo.ANONYMOUS_USER_NAME
                response.author.profileImageUrl shouldBe null
                response.author.id shouldBe 2L
                response.content shouldBe "탈퇴자 댓글"
            }
        }
    })
