package com.weeth.domain.club.application.usecase.query

import com.weeth.domain.club.application.mapper.ClubMapper
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberCardinalReader
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.domain.entity.UserProfile
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.test.util.ReflectionTestUtils

class GetClubQueryServiceTest :
    DescribeSpec({
        val clubReader = mockk<ClubReader>()
        val clubMemberReader = mockk<ClubMemberReader>()
        val clubMemberCardinalReader = mockk<ClubMemberCardinalReader>()
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>()
        val fileAccessUrlPort = mockk<FileAccessUrlPort>()
        val clubMapper = ClubMapper(fileAccessUrlPort)
        val queryService =
            GetClubQueryService(
                clubReader = clubReader,
                clubMemberReader = clubMemberReader,
                clubMemberCardinalReader = clubMemberCardinalReader,
                clubPermissionPolicy = clubPermissionPolicy,
                clubMapper = clubMapper,
            )

        beforeTest {
            clearMocks(clubReader, clubMemberReader, clubMemberCardinalReader, clubPermissionPolicy, fileAccessUrlPort)
        }

        describe("findMyClubs") {
            it("내 동아리 목록에 현재 사용 중인 프로필을 포함한다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                val club = ClubTestFixture.createClub(id = 100L, name = "Leets")
                val member = ClubMemberTestFixture.createActiveMember(id = 10L, club = club, user = user)
                val profile =
                    UserProfile
                        .create(
                            user = user,
                            name = "길동",
                            profileImageStorageKey = "USER_PROFILE_IMAGE/2026-07/profile.png",
                            bio = "안녕하세요",
                        ).apply {
                            ReflectionTestUtils.setField(this, "id", 20L)
                        }
                member.assignProfile(profile)
                every { clubMemberReader.findAllByUserIdAndMemberStatusWithClub(1L, MemberStatus.ACTIVE) } returns
                    listOf(member)
                every { clubMemberCardinalReader.findAllByClubMembers(listOf(member)) } returns emptyList()
                every { clubMemberReader.countActiveByClubId(100L) } returns 12L
                every { fileAccessUrlPort.resolve("USER_PROFILE_IMAGE/2026-07/profile.png") } returns
                    "https://cdn.test/profile.png"

                val responses = queryService.findMyClubs(1L)

                responses.single().usingProfile?.profileId shouldBe 20L
                responses.single().usingProfile?.name shouldBe "길동"
                responses.single().usingProfile?.profileImageUrl shouldBe "https://cdn.test/profile.png"
                responses.single().usingProfile?.bio shouldBe "안녕하세요"
            }
        }
    })
