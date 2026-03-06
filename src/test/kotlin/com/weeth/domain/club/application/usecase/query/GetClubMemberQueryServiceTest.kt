package com.weeth.domain.club.application.usecase.query

import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.club.application.mapper.ClubMapper
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.repository.ClubMemberCardinalReader
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class GetClubMemberQueryServiceTest :
    DescribeSpec({
        val clubMemberReader = mockk<ClubMemberReader>()
        val clubMemberCardinalReader = mockk<ClubMemberCardinalReader>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val clubMapper = ClubMapper()

        val service =
            GetClubMemberQueryService(
                clubMemberReader = clubMemberReader,
                clubMemberCardinalReader = clubMemberCardinalReader,
                clubMemberPolicy = clubMemberPolicy,
                clubMapper = clubMapper,
            )

        describe("findClubMembersForAdmin") {
            context("관리자가 멤버 목록을 조회하는 경우") {
                it("각 멤버의 소속 기수 정보를 함께 반환한다") {
                    val club = ClubTestFixture.createClub(id = 1L)
                    val admin = ClubTestFixture.createClubMember(club = club, memberRole = MemberRole.ADMIN)
                    val member = ClubTestFixture.createClubMember(id = 11L, club = club, user = UserTestFixture.createActiveUser1(id = 3L))
                    val cardinal7 = Cardinal.create(cardinalNumber = 7)
                    val cardinal6 = Cardinal.create(cardinalNumber = 6)
                    val memberCardinals =
                        listOf(
                            ClubMemberCardinal.create(member, cardinal7),
                            ClubMemberCardinal.create(member, cardinal6),
                        )

                    every { clubMemberPolicy.requireAdmin(1L, 99L) } returns admin
                    every { clubMemberReader.findAllByClubId(1L) } returns listOf(member)
                    every { clubMemberCardinalReader.findAllByClubMembers(listOf(member)) } returns memberCardinals

                    val result = service.findClubMembersForAdmin(clubId = 1L, userId = 99L)

                    result shouldHaveSize 1
                    result.first().clubMemberId shouldBe 11L
                    result.first().cardinals shouldBe listOf(6, 7)
                    verify(exactly = 1) { clubMemberPolicy.requireAdmin(1L, 99L) }
                    verify(exactly = 1) { clubMemberCardinalReader.findAllByClubMembers(listOf(member)) }
                }
            }
        }
    })
