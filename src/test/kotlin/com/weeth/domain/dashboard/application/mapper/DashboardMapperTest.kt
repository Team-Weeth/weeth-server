package com.weeth.domain.dashboard.application.mapper

import com.weeth.domain.board.domain.entity.Board
import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.application.dto.response.UserInfo
import com.weeth.domain.user.application.mapper.UserInfoMapper
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.UserProfile
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

class DashboardMapperTest :
    DescribeSpec({
        val fileMapper = mockk<FileMapper>()
        val fileAccessUrlPort = mockk<FileAccessUrlPort>()
        val userInfoMapper = UserInfoMapper(fileAccessUrlPort)
        val mapper = DashboardMapper(fileMapper, fileAccessUrlPort, userInfoMapper)
        val now = LocalDateTime.now()

        val board = mockk<Board>()
        every { board.id } returns 10L
        every { board.canWriteBy(any()) } returns true
        every { board.isCommentEnabled } returns true

        describe("toMyInfoResponse") {
            it("사용 중인 멀티프로필 기준으로 userInfo를 반환한다") {
                val user = UserTestFixture.createActiveUser1(10L)
                val member = ClubTestFixture.createClubMember(user = user)
                member.updateProfileImageUrl("CLUB_MEMBER_PROFILE/legacy.png")
                val userProfile =
                    UserProfile.create(
                        user = user,
                        name = "대시 프로필",
                        profileImageStorageKey = "USER_PROFILE_IMAGE/dashboard.png",
                    )
                member.assignProfile(userProfile)

                every { fileAccessUrlPort.resolve("USER_PROFILE_IMAGE/dashboard.png") } returns
                    "https://cdn/dashboard-profile.png"
                every { fileAccessUrlPort.resolve("CLUB_MEMBER_PROFILE/legacy.png") } returns
                    "https://cdn/legacy-profile.png"

                val response = mapper.toMyInfoResponse(member)

                response.userInfo.id shouldBe 10L
                response.userInfo.name shouldBe "대시 프로필"
                response.userInfo.profileImageUrl shouldBe "https://cdn/dashboard-profile.png"
                response.userInfo.role shouldBe MemberRole.USER
            }

            it("사용 중인 멀티프로필이 없으면 기본 사용자 정보로 반환한다") {
                val user = UserTestFixture.createActiveUser1(10L)
                val member = ClubTestFixture.createClubMember(user = user)
                member.updateProfileImageUrl("CLUB_MEMBER_PROFILE/default.png")

                every { fileAccessUrlPort.resolve("CLUB_MEMBER_PROFILE/default.png") } returns
                    "https://cdn/default-profile.png"

                val response = mapper.toMyInfoResponse(member)

                response.userInfo.id shouldBe 10L
                response.userInfo.name shouldBe "적순"
                response.userInfo.profileImageUrl shouldBe "https://cdn/default-profile.png"
                response.userInfo.role shouldBe MemberRole.USER
            }
        }

        describe("toPostResponse 작성자 익명화") {
            it("LEFT 멤버 게시글은 작성자 이름이 익명 라벨로 치환되고 프로필이 null이 된다") {
                every { fileAccessUrlPort.resolve("CLUB_MEMBER_PROFILE/leak.png") } returns "https://cdn/leak.png"
                val leftUser = mockk<User>()
                every { leftUser.id } returns 9L
                every { leftUser.name } returns "노출되면안됨"

                val leftMember = mockk<ClubMember>()
                every { leftMember.memberRole } returns MemberRole.USER
                every { leftMember.memberStatus } returns MemberStatus.LEFT
                every { leftMember.profileImageStorageKey } returns "CLUB_MEMBER_PROFILE/leak.png"
                every { leftMember.userProfile } returns null
                every { leftMember.user } returns leftUser

                val post = mockk<Post>()
                every { post.id } returns 200L
                every { post.title } returns "탈퇴자 글"
                every { post.content } returns "내용"
                every { post.clubMember } returns leftMember
                every { post.board } returns board
                every { post.commentCount } returns 0
                every { post.likeCount } returns 0
                every { post.createdAt } returns now

                val response =
                    mapper.toPostResponse(
                        post,
                        files = emptyList(),
                        now = now,
                        isLiked = false,
                        memberRole = MemberRole.USER,
                    )

                response.author.name shouldBe UserInfo.ANONYMOUS_USER_NAME
                response.author.profileImageUrl shouldBe null
                response.author.id shouldBe 9L
            }

            it("ACTIVE 멤버 게시글은 사용 중인 멀티프로필 기준으로 작성자 정보가 노출된다") {
                every { fileAccessUrlPort.resolve("USER_PROFILE_IMAGE/dashboard.png") } returns
                    "https://cdn/dashboard-profile.png"
                every { fileAccessUrlPort.resolve("CLUB_MEMBER_PROFILE/legacy.png") } returns
                    "https://cdn/legacy-profile.png"
                val user = mockk<User>()
                every { user.id } returns 10L
                every { user.name } returns "개인정보 이름"
                val userProfile =
                    UserProfile.create(
                        user = user,
                        name = "대시 프로필",
                        profileImageStorageKey = "USER_PROFILE_IMAGE/dashboard.png",
                    )

                val activeMember = mockk<ClubMember>()
                every { activeMember.memberRole } returns MemberRole.USER
                every { activeMember.memberStatus } returns MemberStatus.ACTIVE
                every { activeMember.profileImageStorageKey } returns "CLUB_MEMBER_PROFILE/legacy.png"
                every { activeMember.userProfile } returns userProfile
                every { activeMember.user } returns user

                val post = mockk<Post>()
                every { post.id } returns 201L
                every { post.title } returns "활성 멤버 글"
                every { post.content } returns "내용"
                every { post.clubMember } returns activeMember
                every { post.board } returns board
                every { post.commentCount } returns 0
                every { post.likeCount } returns 0
                every { post.createdAt } returns now

                val response =
                    mapper.toPostResponse(
                        post,
                        files = emptyList(),
                        now = now,
                        isLiked = false,
                        memberRole = MemberRole.USER,
                    )

                response.author.name shouldBe "대시 프로필"
                response.author.profileImageUrl shouldBe "https://cdn/dashboard-profile.png"
                response.author.id shouldBe 10L
            }
        }
    })
