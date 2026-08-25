package com.weeth.domain.club.application.usecase.query

import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.club.application.dto.request.ClubMemberSort
import com.weeth.domain.club.application.exception.ClubMemberNotFoundException
import com.weeth.domain.club.application.exception.ClubMemberNotInClubException
import com.weeth.domain.club.application.mapper.ClubMapper
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberCardinalReader
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.penalty.domain.repository.PenaltyReader
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class GetClubMemberQueryServiceTest :
    DescribeSpec({
        val clubMemberReader = mockk<ClubMemberReader>()
        val clubMemberCardinalReader = mockk<ClubMemberCardinalReader>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>()
        val fileAccessUrlPort = mockk<FileAccessUrlPort>()
        val userReader = mockk<UserReader>()
        val penaltyReader = mockk<PenaltyReader>()
        val clubMapper = ClubMapper(fileAccessUrlPort)

        val service =
            GetClubMemberQueryService(
                clubMemberReader = clubMemberReader,
                clubMemberCardinalReader = clubMemberCardinalReader,
                clubMemberPolicy = clubMemberPolicy,
                clubPermissionPolicy = clubPermissionPolicy,
                clubMapper = clubMapper,
                userReader = userReader,
                penaltyReader = penaltyReader,
            )

        beforeTest {
            clearMocks(
                clubMemberReader,
                clubMemberCardinalReader,
                clubMemberPolicy,
                clubPermissionPolicy,
                userReader,
                penaltyReader,
            )
        }

        describe("searchClubMembers") {
            it("이름으로 멤버를 검색한다") {
                val club = ClubTestFixture.createClub()
                val admin = ClubTestFixture.createClubMember(club = club, memberRole = MemberRole.ADMIN)
                val member = ClubTestFixture.createClubMember(club = club)
                val cardinal = Cardinal.create(club = club, cardinalNumber = 7)
                val memberCardinal = ClubMemberCardinal.create(member, cardinal)

                every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns admin
                every {
                    clubMemberReader.findAdminMembers(1L, null, "홍길동", "CARDINAL_DESC", any())
                } returns PageImpl(listOf(member), PageRequest.of(0, 50), 1)
                every { clubMemberCardinalReader.findAllByClubMembers(listOf(member)) } returns listOf(memberCardinal)
                every { penaltyReader.findByClubMemberIds(any()) } returns emptyList()

                val result =
                    service.searchClubMembers(
                        clubId = 1L,
                        userId = 99L,
                        keyword = "홍길동",
                        cardinalNumber = null,
                    )

                result shouldHaveSize 1
                verify(exactly = 1) {
                    clubMemberReader.findAdminMembers(1L, null, "홍길동", "CARDINAL_DESC", any())
                }
            }

            it("특정 기수에서만 멤버를 검색한다") {
                val club = ClubTestFixture.createClub()
                val admin = ClubTestFixture.createClubMember(club = club, memberRole = MemberRole.ADMIN)
                val member = ClubTestFixture.createClubMember(club = club)

                every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns admin
                every {
                    clubMemberReader.findAdminMembers(1L, 7, "김", "CARDINAL_DESC", any())
                } returns PageImpl(listOf(member), PageRequest.of(0, 50), 1)
                every { clubMemberCardinalReader.findAllByClubMembers(listOf(member)) } returns emptyList()
                every { penaltyReader.findByClubMemberIds(any()) } returns emptyList()

                val result =
                    service.searchClubMembers(
                        clubId = 1L,
                        userId = 99L,
                        keyword = "김",
                        cardinalNumber = 7,
                    )

                result shouldHaveSize 1
            }

            it("검색 결과가 없을 수 있다") {
                val club = ClubTestFixture.createClub()
                val admin = ClubTestFixture.createClubMember(club = club, memberRole = MemberRole.ADMIN)

                every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns admin
                every {
                    clubMemberReader.findAdminMembers(1L, null, "존재하지않음", "CARDINAL_DESC", any())
                } returns PageImpl(emptyList(), PageRequest.of(0, 50), 0)
                every { clubMemberCardinalReader.findAllByClubMembers(emptyList()) } returns emptyList()

                val result =
                    service.searchClubMembers(
                        clubId = 1L,
                        userId = 99L,
                        keyword = "존재하지않음",
                        cardinalNumber = null,
                    )

                result.shouldBeEmpty()
            }

            it("기본값은 기수 내림차순 정렬이다") {
                val club = ClubTestFixture.createClub()
                val admin = ClubTestFixture.createClubMember(club = club, memberRole = MemberRole.ADMIN)

                every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns admin
                every {
                    clubMemberReader.findAdminMembers(1L, null, "김", "CARDINAL_DESC", any())
                } returns PageImpl(emptyList(), PageRequest.of(0, 50), 0)
                every { clubMemberCardinalReader.findAllByClubMembers(emptyList()) } returns emptyList()

                service.searchClubMembers(
                    clubId = 1L,
                    userId = 99L,
                    keyword = "김",
                    cardinalNumber = null,
                )

                verify(exactly = 1) {
                    clubMemberReader.findAdminMembers(1L, null, "김", "CARDINAL_DESC", any())
                }
            }
        }

        describe("findClubMembersForAdmin") {
            context("관리자가 멤버 목록을 조회하는 경우") {
                it("각 멤버의 소속 기수 정보를 페이지 응답으로 반환한다") {
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
                    every {
                        clubMemberReader.findAdminMembers(1L, null, null, "CARDINAL_DESC", any())
                    } returns PageImpl(listOf(member), PageRequest.of(0, 20), 1)
                    every { clubMemberCardinalReader.findAllByClubMembers(listOf(member)) } returns memberCardinals
                    every { penaltyReader.findByClubMemberIds(any()) } returns emptyList()

                    val result =
                        service.findClubMembersForAdmin(
                            clubId = 1L,
                            userId = 99L,
                            page = 0,
                            size = 20,
                            keyword = null,
                            cardinalNumber = null,
                            sort = ClubMemberSort.CARDINAL_DESC,
                        )

                    result.content shouldHaveSize 1
                    result.totalElements shouldBe 1
                    result.pageNumber shouldBe 0
                    val response = result.content.first()
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
                    response.joinedAt shouldBe member.createdAt
                    verify(exactly = 1) { clubPermissionPolicy.requireAdmin(1L, 99L) }
                    verify(exactly = 1) { clubMemberCardinalReader.findAllByClubMembers(listOf(member)) }
                }

                it("검색어 공백을 제거하고 기수 필터·정렬을 그대로 리포지토리에 전달한다") {
                    val club = ClubTestFixture.createClub()
                    val admin = ClubTestFixture.createClubMember(club = club, memberRole = MemberRole.ADMIN)

                    every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns admin
                    every {
                        clubMemberReader.findAdminMembers(1L, 7, "홍길동", "NAME_ASC", any())
                    } returns PageImpl(emptyList(), PageRequest.of(0, 20), 0)
                    every { clubMemberCardinalReader.findAllByClubMembers(emptyList()) } returns emptyList()

                    val result =
                        service.findClubMembersForAdmin(
                            clubId = 1L,
                            userId = 99L,
                            page = 0,
                            size = 20,
                            keyword = "  홍길동  ",
                            cardinalNumber = 7,
                            sort = ClubMemberSort.NAME_ASC,
                        )

                    result.content.shouldBeEmpty()
                    verify(exactly = 1) {
                        clubMemberReader.findAdminMembers(1L, 7, "홍길동", "NAME_ASC", any())
                    }
                }

                it("페이지 크기를 1~100으로 보정한다") {
                    val club = ClubTestFixture.createClub()
                    val admin = ClubTestFixture.createClubMember(club = club, memberRole = MemberRole.ADMIN)
                    val pageableSlot = slot<Pageable>()

                    every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns admin
                    every {
                        clubMemberReader.findAdminMembers(1L, null, null, any(), capture(pageableSlot))
                    } returns PageImpl(emptyList(), PageRequest.of(0, 100), 0)
                    every { clubMemberCardinalReader.findAllByClubMembers(emptyList()) } returns emptyList()

                    service.findClubMembersForAdmin(
                        clubId = 1L,
                        userId = 99L,
                        page = -5,
                        size = 500,
                        keyword = null,
                        cardinalNumber = null,
                        sort = ClubMemberSort.CARDINAL_DESC,
                    )

                    pageableSlot.captured.pageNumber shouldBe 0
                    pageableSlot.captured.pageSize shouldBe 100
                }
            }
        }

        describe("findClubMemberDetailForAdmin") {
            val club = ClubTestFixture.createClub(id = 1L)
            val admin = ClubTestFixture.createClubMember(club = club, memberRole = MemberRole.ADMIN)

            context("추방된 멤버를 조회하는 경우") {
                it("상태와 무관하게 상세 정보를 반환한다") {
                    val member = ClubMemberTestFixture.createBannedMember(id = 5L, club = club)

                    every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns admin
                    every { clubMemberReader.findAdminMemberDetail(5L) } returns member
                    every { clubMemberCardinalReader.findAllByClubMember(member) } returns emptyList()

                    val result = service.findClubMemberDetailForAdmin(clubId = 1L, userId = 99L, clubMemberId = 5L)

                    result.clubMemberId shouldBe 5L
                    result.memberStatus shouldBe MemberStatus.BANNED
                    result.cardinals.shouldBeEmpty()
                }
            }

            context("멤버가 존재하지 않는 경우") {
                it("ClubMemberNotFoundException을 던진다") {
                    every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns admin
                    every { clubMemberReader.findAdminMemberDetail(5L) } returns null

                    shouldThrow<ClubMemberNotFoundException> {
                        service.findClubMemberDetailForAdmin(clubId = 1L, userId = 99L, clubMemberId = 5L)
                    }
                }
            }

            context("다른 동아리의 멤버인 경우") {
                it("ClubMemberNotInClubException을 던진다") {
                    val otherClub = ClubTestFixture.createClub(id = 2L, code = "OTHER")
                    val member = ClubMemberTestFixture.createActiveMember(id = 5L, club = otherClub)

                    every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns admin
                    every { clubMemberReader.findAdminMemberDetail(5L) } returns member

                    shouldThrow<ClubMemberNotInClubException> {
                        service.findClubMemberDetailForAdmin(clubId = 1L, userId = 99L, clubMemberId = 5L)
                    }
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
