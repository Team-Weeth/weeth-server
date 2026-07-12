package com.weeth.domain.user.application.usecase.query

import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.application.exception.UserProfileNotFoundException
import com.weeth.domain.user.application.mapper.UserProfileMapper
import com.weeth.domain.user.domain.entity.UserProfile
import com.weeth.domain.user.domain.repository.UserProfileRepository
import com.weeth.domain.user.fixture.UserTestFixture
import com.weeth.global.common.id.TsidBase62Encoder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.test.util.ReflectionTestUtils
import java.util.Optional

class GetUserProfileQueryServiceTest :
    DescribeSpec({
        val userProfileRepository = mockk<UserProfileRepository>()
        val clubMemberReader = mockk<ClubMemberReader>()
        val fileAccessUrlPort = mockk<FileAccessUrlPort>()
        val userProfileMapper = UserProfileMapper(fileAccessUrlPort)
        val queryService =
            GetUserProfileQueryService(
                userProfileRepository = userProfileRepository,
                clubMemberReader = clubMemberReader,
                userProfileMapper = userProfileMapper,
            )

        beforeTest {
            clearMocks(userProfileRepository, clubMemberReader, fileAccessUrlPort)
        }

        describe("findAll") {
            it("로그인 사용자의 프로필 목록을 ID 오름차순으로 반환한다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                val first = userProfile(user, id = 10L, name = "길동")
                val second = userProfile(user, id = 11L, name = "위드")
                val club1 = ClubTestFixture.createClub(id = 100L, name = "동아리1")
                val club2 = ClubTestFixture.createClub(id = 101L, name = "동아리2")
                val member1 = ClubMemberTestFixture.createActiveMember(id = 1000L, club = club1, user = user)
                val member2 = ClubMemberTestFixture.createActiveMember(id = 1001L, club = club2, user = user)
                member1.assignProfile(first)
                member2.assignProfile(first)
                every { userProfileRepository.findAllByUserIdOrderByIdAsc(1L) } returns listOf(first, second)
                every {
                    clubMemberReader.findAllByUserIdAndMemberStatusWithClubAndUserProfile(1L, MemberStatus.ACTIVE)
                } returns listOf(member1, member2)

                val result = queryService.findAll(userId = 1L)

                result.profiles shouldHaveSize 2
                result.profiles[0].profileId shouldBe 10L
                result.profiles[0].name shouldBe "길동"
                result.profiles[0].usingClubs shouldHaveSize 2
                result.profiles[0].usingClubs[0].clubId shouldBe TsidBase62Encoder.encode(100L)
                result.profiles[0].usingClubs[0].name shouldBe "동아리1"
                result.profiles[0].usingClubs[1].clubId shouldBe TsidBase62Encoder.encode(101L)
                result.profiles[0].usingClubs[1].name shouldBe "동아리2"
                result.profiles[1].profileId shouldBe 11L
                result.profiles[1].name shouldBe "위드"
                result.profiles[1].usingClubs shouldHaveSize 0
            }
        }

        describe("find") {
            it("로그인 사용자 소유 프로필을 단건 조회한다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                val profile =
                    userProfile(
                        user = user,
                        id = 10L,
                        name = "길동",
                        profileImageStorageKey = "USER_PROFILE_IMAGE/2026-07/profile.png",
                    )
                val club = ClubTestFixture.createClub(id = 100L, name = "동아리")
                val member = ClubMemberTestFixture.createActiveMember(id = 1000L, club = club, user = user)
                member.assignProfile(profile)
                every { userProfileRepository.findByIdAndUserId(10L, 1L) } returns Optional.of(profile)
                every {
                    clubMemberReader.findAllByUserIdAndMemberStatusWithClubAndUserProfile(1L, MemberStatus.ACTIVE)
                } returns listOf(member)
                every { fileAccessUrlPort.resolve("USER_PROFILE_IMAGE/2026-07/profile.png") } returns
                    "https://cdn.test/profile.png"

                val result = queryService.find(userId = 1L, profileId = 10L)

                result.profileId shouldBe 10L
                result.name shouldBe "길동"
                result.profileImageUrl shouldBe "https://cdn.test/profile.png"
                result.usingClubs shouldHaveSize 1
                result.usingClubs[0].clubId shouldBe TsidBase62Encoder.encode(100L)
                result.usingClubs[0].name shouldBe "동아리"
            }

            it("프로필이 없거나 로그인 사용자 소유가 아니면 예외가 발생한다") {
                every { userProfileRepository.findByIdAndUserId(10L, 1L) } returns Optional.empty()

                shouldThrow<UserProfileNotFoundException> {
                    queryService.find(userId = 1L, profileId = 10L)
                }
            }
        }
    }) {
    companion object {
        private fun userProfile(
            user: com.weeth.domain.user.domain.entity.User,
            id: Long,
            name: String,
            profileImageStorageKey: String? = null,
        ): UserProfile =
            UserProfile
                .create(
                    user = user,
                    name = name,
                    profileImageStorageKey = profileImageStorageKey,
                ).apply {
                    ReflectionTestUtils.setField(this, "id", id)
                }
    }
}
