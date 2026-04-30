package com.weeth.domain.club.application.usecase.query

import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.club.application.mapper.ClubMapper
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.repository.ClubMemberCardinalReader
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class GetClubMemberQueryServiceTest :
    DescribeSpec({
        val clubMemberReader = mockk<ClubMemberReader>()
        val clubMemberCardinalReader = mockk<ClubMemberCardinalReader>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>()
        val fileAccessUrlPort = mockk<FileAccessUrlPort>()
        val userReader = mockk<UserReader>()
        val clubMapper = ClubMapper(fileAccessUrlPort)

        val service =
            GetClubMemberQueryService(
                clubMemberReader = clubMemberReader,
                clubMemberCardinalReader = clubMemberCardinalReader,
                clubMemberPolicy = clubMemberPolicy,
                clubPermissionPolicy = clubPermissionPolicy,
                clubMapper = clubMapper,
                userReader = userReader,
            )

        beforeTest {
            clearMocks(clubMemberReader, clubMemberCardinalReader, clubMemberPolicy, clubPermissionPolicy, userReader)
        }

        describe("findClubMembersForAdmin") {
            context("관리자가 멤버 목록을 조회하는 경우") {
                it("각 멤버의 소속 기수 정보를 함께 반환한다") {
                    val club = ClubTestFixture.createClub()
                    val admin = ClubTestFixture.createClubMember(club = club, memberRole = MemberRole.ADMIN)
                    val member =
                        ClubTestFixture.createClubMember(club = club, user = UserTestFixture.createActiveUser1(1L))
                    val cardinal7 = Cardinal.create(club = club, cardinalNumber = 7)
                    val cardinal6 = Cardinal.create(club = club, cardinalNumber = 6)
                    val memberCardinals =
                        listOf(
                            ClubMemberCardinal.create(member, cardinal7),
                            ClubMemberCardinal.create(member, cardinal6),
                        )

                    every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns admin
                    every { clubMemberReader.findAllByClubId(1L) } returns listOf(member)
                    every { clubMemberCardinalReader.findAllByClubMembers(listOf(member)) } returns memberCardinals

                    val result = service.findClubMembersForAdmin(clubId = 1L, userId = 99L)

                    result shouldHaveSize 1
                    val response = result.first()
                    response.name shouldBe member.user.name
                    response.email shouldBe member.user.emailValue
                    response.studentId shouldBe member.user.studentId
                    response.tel shouldBe member.user.telValue
                    response.department shouldBe member.user.department
                    response.memberStatus shouldBe member.memberStatus
                    response.memberRole shouldBe member.memberRole
                    response.attendanceCount shouldBe member.attendanceStats.attendanceCount
                    response.absenceCount shouldBe member.attendanceStats.absenceCount
                    response.attendanceRate shouldBe member.attendanceStats.attendanceRate
                    response.penaltyCount shouldBe member.penaltyCount
                    response.cardinals shouldBe listOf(6, 7)
                    verify(exactly = 1) { clubPermissionPolicy.requireAdmin(1L, 99L) }
                    verify(exactly = 1) { clubMemberCardinalReader.findAllByClubMembers(listOf(member)) }
                }
            }
        }

        describe("findProfileStatus") {
            val club = ClubTestFixture.createClub()
            val clubId = 1L
            val userId = 1L

            context("프로필이 완성되고 기수가 등록된 경우") {
                it("profileCompleted=true, cardinalAssigned=true, missingFields 비어있음") {
                    val user =
                        User.create(
                            name = "test",
                            email = "test@test.com",
                            studentId = "20200001",
                            tel = "01012345678",
                            school = "가천대학교",
                            department = "CS",
                        )
                    val member = ClubMemberTestFixture.createActiveMember(club = club, user = user)
                    val cardinal = Cardinal.create(club = club, cardinalNumber = 7)
                    val memberCardinal = ClubMemberCardinal.create(member, cardinal)

                    every { clubMemberPolicy.getActiveMember(clubId, userId) } returns member
                    every { userReader.getById(userId) } returns user
                    every { clubMemberCardinalReader.findLatestCardinalByClubMember(member) } returns memberCardinal

                    val result = service.findProfileStatus(clubId, userId)

                    result.profileCompleted shouldBe true
                    result.cardinalAssigned shouldBe true
                    result.missingFields.shouldBeEmpty()
                }
            }

            context("프로필이 미완성이고 기수가 미등록인 경우") {
                it("profileCompleted=false, cardinalAssigned=false, missingFields에 비어있는 필드 반환") {
                    val user = User.create(name = "test", email = "test@test.com")
                    val member = ClubMemberTestFixture.createActiveMember(club = club, user = user)

                    every { clubMemberPolicy.getActiveMember(clubId, userId) } returns member
                    every { userReader.getById(userId) } returns user
                    every { clubMemberCardinalReader.findLatestCardinalByClubMember(member) } returns null

                    val result = service.findProfileStatus(clubId, userId)

                    result.profileCompleted shouldBe false
                    result.cardinalAssigned shouldBe false
                    result.missingFields shouldContainExactlyInAnyOrder
                        listOf("studentId", "tel", "school", "department")
                }
            }

            context("프로필은 완성이나 기수가 미등록인 경우") {
                it("profileCompleted=true, cardinalAssigned=false") {
                    val user =
                        User.create(
                            name = "test",
                            email = "test@test.com",
                            studentId = "20200001",
                            tel = "01012345678",
                            school = "가천대학교",
                            department = "CS",
                        )
                    val member = ClubMemberTestFixture.createActiveMember(club = club, user = user)

                    every { clubMemberPolicy.getActiveMember(clubId, userId) } returns member
                    every { userReader.getById(userId) } returns user
                    every { clubMemberCardinalReader.findLatestCardinalByClubMember(member) } returns null

                    val result = service.findProfileStatus(clubId, userId)

                    result.profileCompleted shouldBe true
                    result.cardinalAssigned shouldBe false
                    result.missingFields.shouldBeEmpty()
                }
            }
        }
    })
