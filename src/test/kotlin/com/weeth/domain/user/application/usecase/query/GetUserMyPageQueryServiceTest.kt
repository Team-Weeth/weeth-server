package com.weeth.domain.user.application.usecase.query

import com.weeth.domain.attendance.domain.enums.AttendanceStatus
import com.weeth.domain.attendance.domain.repository.AttendanceReader
import com.weeth.domain.board.domain.repository.PostReader
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.application.mapper.UserMyPageMapper
import com.weeth.domain.user.domain.entity.UserProfile
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.fixture.UserTestFixture
import com.weeth.global.common.id.TsidBase62Encoder
import com.weeth.global.common.vo.PhoneNumber
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.test.util.ReflectionTestUtils

class GetUserMyPageQueryServiceTest :
    DescribeSpec({
        val userReader = mockk<UserReader>()
        val clubMemberReader = mockk<ClubMemberReader>()
        val postReader = mockk<PostReader>()
        val attendanceReader = mockk<AttendanceReader>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val fileAccessUrlPort = mockk<FileAccessUrlPort>()
        val userMyPageMapper = UserMyPageMapper(fileAccessUrlPort)
        val queryService =
            GetUserMyPageQueryService(
                userReader = userReader,
                clubMemberReader = clubMemberReader,
                postReader = postReader,
                attendanceReader = attendanceReader,
                clubMemberPolicy = clubMemberPolicy,
                userMyPageMapper = userMyPageMapper,
            )

        beforeTest {
            clearMocks(userReader, clubMemberReader, postReader, attendanceReader, clubMemberPolicy, fileAccessUrlPort)
        }

        describe("getMyPage") {
            it("기존 마이페이지 요약과 현재 동아리에서 사용 중인 멀티프로필을 반환한다") {
                val user =
                    UserTestFixture
                        .createRegisteredUser(1L)
                        .also {
                            it.update(
                                name = "홍길동",
                                studentId = "20201234",
                                tel = PhoneNumber.from("01012345678"),
                                school = "가천대학교",
                                department = "컴퓨터공학과",
                            )
                        }
                val profile =
                    UserProfile
                        .create(
                            user = user,
                            name = "길동",
                            profileImageStorageKey = "USER_PROFILE_IMAGE/profile.png",
                            headerImageStorageKey = "USER_PROFILE_HEADER/header.png",
                            bio = "안녕하세요",
                        ).apply {
                            ReflectionTestUtils.setField(this, "id", 10L)
                        }
                val firstClub = ClubTestFixture.createClub(id = 100L, name = "Leets")
                val secondClub = ClubTestFixture.createClub(id = 101L, name = "Weeth")
                val firstMember =
                    ClubTestFixture
                        .createClubMember(club = firstClub, user = user)
                        .withId(1000L)
                        .withProfile(profile)
                val secondMember =
                    ClubTestFixture
                        .createClubMember(club = secondClub, user = user)
                        .withId(1001L)
                        .withProfile(profile)

                every { userReader.getById(1L) } returns user
                every { clubMemberReader.findAllByUserIdWithClubAndUserProfile(1L) } returns
                    listOf(firstMember, secondMember)
                every { clubMemberPolicy.getActiveMember(100L, 1L) } returns firstMember
                every { postReader.countActiveByClubMemberIds(listOf(1000L)) } returns 12L
                every {
                    attendanceReader.countByClubMemberIdsAndStatus(
                        listOf(1000L),
                        AttendanceStatus.ATTEND,
                    )
                } returns 8L
                every { fileAccessUrlPort.resolve("USER_PROFILE_IMAGE/profile.png") } returns
                    "https://cdn.test/profile.png"
                every { fileAccessUrlPort.resolve("USER_PROFILE_HEADER/header.png") } returns
                    "https://cdn.test/header.png"

                val result = queryService.getMyPage(userId = 1L, clubId = 100L)

                result.user.name shouldBe "홍길동"
                result.user.tel shouldBe "01012345678"
                result.user.email shouldBe "registered@test.com"
                result.user.school shouldBe "가천대학교"
                result.user.department shouldBe "컴퓨터공학과"
                result.user.studentId shouldBe "20201234"
                result.stats.postCount shouldBe 12L
                result.stats.attendedSessionCount shouldBe 8L
                result.usingProfiles shouldHaveSize 1
                result.usingProfiles[0].profileId shouldBe 10L
                result.usingProfiles[0].name shouldBe "길동"
                result.usingProfiles[0].profileImageUrl shouldBe "https://cdn.test/profile.png"
                result.usingProfiles[0].headerImageUrl shouldBe "https://cdn.test/header.png"
                result.usingProfiles[0].bio shouldBe "안녕하세요"
                result.usingProfiles[0].clubs.map { it.clubId } shouldBe
                    listOf(TsidBase62Encoder.encode(100L), TsidBase62Encoder.encode(101L))
                result.usingProfiles[0].clubs.map { it.name } shouldBe listOf("Leets", "Weeth")
                result.currentProfile?.profileId shouldBe 10L
                result.currentProfile?.name shouldBe "길동"
                result.currentProfile?.profileImageUrl shouldBe "https://cdn.test/profile.png"
                result.currentProfile?.headerImageUrl shouldBe "https://cdn.test/header.png"
                result.currentProfile?.bio shouldBe "안녕하세요"
            }
        }
    }) {
    companion object {
        private fun ClubMember.withId(id: Long): ClubMember =
            apply {
                ReflectionTestUtils.setField(this, "id", id)
            }

        private fun ClubMember.withProfile(profile: UserProfile): ClubMember =
            apply {
                assignProfile(profile)
            }
    }
}
