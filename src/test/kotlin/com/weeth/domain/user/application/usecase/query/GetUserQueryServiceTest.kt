package com.weeth.domain.user.application.usecase.query

import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.user.application.dto.response.UserDetailsResponse
import com.weeth.domain.user.application.dto.response.UserProfileResponse
import com.weeth.domain.user.application.dto.response.UserSummaryResponse
import com.weeth.domain.user.application.mapper.UserMapper
import com.weeth.domain.user.domain.entity.UserCardinal
import com.weeth.domain.user.domain.repository.UserCardinalRepository
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class GetUserQueryServiceTest :
    DescribeSpec({
        val userRepository = mockk<UserRepository>()
        val cardinalReader = mockk<CardinalReader>()
        val userCardinalRepository = mockk<UserCardinalRepository>()
        val mapper = mockk<UserMapper>()

        val queryService =
            GetUserQueryService(
                userRepository,
                cardinalReader,
                userCardinalRepository,
                mapper,
            )

        describe("existsByEmail") {
            it("repository exists 결과를 반환한다") {
                every { userRepository.existsByEmailValue("foo@bar.com") } returns true

                queryService.existsByEmail("foo@bar.com") shouldBe true
            }
        }

        describe("findUserDetails") {
            it("user와 cardinal 목록을 조회해 UserDetailsResponse로 매핑한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val cardinal =
                    CardinalTestFixture.createCardinal(
                        id = 10L,
                        cardinalNumber = 6,
                        year = 2024,
                        semester = 2,
                    )
                val userCardinals = listOf(UserCardinal.create(user, cardinal))
                val response =
                    UserDetailsResponse(
                        1,
                        user.name,
                        user.emailValue,
                        user.studentId,
                        user.department,
                        listOf(6),
                        user.role,
                        user.bio,
                        user.profileImageUrl,
                    )

                every { userRepository.getById(1L) } returns user
                every { userCardinalRepository.findAllByUser(user) } returns userCardinals
                every { mapper.toUserDetailsResponse(user, userCardinals) } returns response

                queryService.findUserDetails(1L) shouldBe response
            }
        }

        describe("findMyProfile") {
            it("내 프로필을 UserProfileResponse로 매핑한다") {
                val user = UserTestFixture.createActiveUser1(2L)
                val cardinal =
                    CardinalTestFixture.createCardinal(
                        id = 11L,
                        cardinalNumber = 7,
                        year = 2025,
                        semester = 1,
                    )
                val userCardinals = listOf(UserCardinal.create(user, cardinal))
                val response =
                    UserProfileResponse(
                        2,
                        user.name,
                        user.emailValue,
                        user.studentId,
                        user.telValue,
                        user.department,
                        listOf(7),
                        user.role,
                        user.bio,
                        user.profileImageUrl,
                    )

                every { userRepository.getById(2L) } returns user
                every { userCardinalRepository.findAllByUser(user) } returns userCardinals
                every { mapper.toUserProfileResponse(user, userCardinals) } returns response

                queryService.findMyProfile(2L) shouldBe response
            }
        }

        describe("findMyInfo") {
            it("내 정보를 UserSummaryResponse로 매핑한다") {
                val user = UserTestFixture.createActiveUser1(3L)
                val cardinal =
                    CardinalTestFixture.createCardinal(
                        id = 12L,
                        cardinalNumber = 8,
                        year = 2025,
                        semester = 2,
                    )
                val userCardinals = listOf(UserCardinal.create(user, cardinal))
                val response =
                    UserSummaryResponse(
                        3,
                        user.name,
                        listOf(8),
                        user.role,
                    )

                every { userRepository.getById(3L) } returns user
                every { userCardinalRepository.findAllByUser(user) } returns userCardinals
                every { mapper.toUserSummaryResponse(user, userCardinals) } returns response

                queryService.findMyInfo(3L) shouldBe response
            }
        }
    })
