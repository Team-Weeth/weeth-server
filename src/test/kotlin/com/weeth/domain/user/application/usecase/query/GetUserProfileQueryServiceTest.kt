package com.weeth.domain.user.application.usecase.query

import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.application.exception.UserProfileNotFoundException
import com.weeth.domain.user.application.mapper.UserProfileMapper
import com.weeth.domain.user.domain.entity.UserProfile
import com.weeth.domain.user.domain.repository.UserProfileRepository
import com.weeth.domain.user.fixture.UserTestFixture
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
        val fileAccessUrlPort = mockk<FileAccessUrlPort>()
        val userProfileMapper = UserProfileMapper(fileAccessUrlPort)
        val queryService =
            GetUserProfileQueryService(
                userProfileRepository = userProfileRepository,
                userProfileMapper = userProfileMapper,
            )

        beforeTest {
            clearMocks(userProfileRepository, fileAccessUrlPort)
        }

        describe("findAll") {
            it("로그인 사용자의 프로필 목록을 ID 오름차순으로 반환한다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                val first = userProfile(user, id = 10L, name = "길동")
                val second = userProfile(user, id = 11L, name = "위드")
                every { userProfileRepository.findAllByUserIdOrderByIdAsc(1L) } returns listOf(first, second)

                val result = queryService.findAll(userId = 1L)

                result.profiles shouldHaveSize 2
                result.profiles[0].profileId shouldBe 10L
                result.profiles[0].name shouldBe "길동"
                result.profiles[1].profileId shouldBe 11L
                result.profiles[1].name shouldBe "위드"
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
                every { userProfileRepository.findByIdAndUserId(10L, 1L) } returns Optional.of(profile)
                every { fileAccessUrlPort.resolve("USER_PROFILE_IMAGE/2026-07/profile.png") } returns
                    "https://cdn.test/profile.png"

                val result = queryService.find(userId = 1L, profileId = 10L)

                result.profileId shouldBe 10L
                result.name shouldBe "길동"
                result.profileImageUrl shouldBe "https://cdn.test/profile.png"
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
