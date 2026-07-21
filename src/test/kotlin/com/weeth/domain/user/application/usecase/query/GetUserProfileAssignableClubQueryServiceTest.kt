package com.weeth.domain.user.application.usecase.query

import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberCount
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.fixture.UserTestFixture
import com.weeth.global.common.id.TsidBase62Encoder
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.test.util.ReflectionTestUtils

class GetUserProfileAssignableClubQueryServiceTest :
    DescribeSpec({
        val clubMemberReader = mockk<ClubMemberReader>()
        val fileAccessUrlPort = mockk<FileAccessUrlPort>()
        val queryService =
            GetUserProfileAssignableClubQueryService(
                clubMemberReader = clubMemberReader,
                fileAccessUrlPort = fileAccessUrlPort,
            )

        beforeTest {
            clearMocks(clubMemberReader, fileAccessUrlPort)
        }

        describe("findAll") {
            it("로그인 사용자가 활동 중인 동아리 목록을 프로필 사용 대상 응답으로 반환한다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                val firstClub =
                    ClubTestFixture
                        .createClub(id = 100L, name = "Leets")
                        .withProfileImage("CLUB_PROFILE/leet.png")
                val secondClub =
                    ClubTestFixture
                        .createClub(id = 101L, name = "Weeth")
                        .withProfileImage("CLUB_PROFILE/weeth.png")
                val firstMember = ClubMemberTestFixture.createActiveMember(id = 1000L, club = firstClub, user = user)
                val secondMember = ClubMemberTestFixture.createActiveMember(id = 1001L, club = secondClub, user = user)

                every {
                    clubMemberReader.findAllByUserIdAndMemberStatusWithClub(1L, MemberStatus.ACTIVE)
                } returns listOf(secondMember, firstMember)
                every { clubMemberReader.countActiveByClubIds(listOf(100L, 101L)) } returns
                    listOf(
                        ClubMemberCount(clubId = 100L, memberCount = 12L),
                        ClubMemberCount(clubId = 101L, memberCount = 8L),
                    )
                every { fileAccessUrlPort.resolve("CLUB_PROFILE/leet.png") } returns "https://cdn.test/leet.png"
                every { fileAccessUrlPort.resolve("CLUB_PROFILE/weeth.png") } returns "https://cdn.test/weeth.png"

                val result = queryService.findAll(userId = 1L)

                result.clubs shouldHaveSize 2
                result.clubs[0].clubId shouldBe TsidBase62Encoder.encode(100L)
                result.clubs[0].name shouldBe "Leets"
                result.clubs[0].clubImage shouldBe "https://cdn.test/leet.png"
                result.clubs[0].clubMemberNumber shouldBe 12L
                result.clubs[1].clubId shouldBe TsidBase62Encoder.encode(101L)
                result.clubs[1].name shouldBe "Weeth"
                result.clubs[1].clubImage shouldBe "https://cdn.test/weeth.png"
                result.clubs[1].clubMemberNumber shouldBe 8L
            }
        }
    }) {
    companion object {
        private fun com.weeth.domain.club.domain.entity.Club.withProfileImage(storageKey: String) =
            apply {
                ReflectionTestUtils.setField(this, "profileImageStorageKey", storageKey)
            }
    }
}
