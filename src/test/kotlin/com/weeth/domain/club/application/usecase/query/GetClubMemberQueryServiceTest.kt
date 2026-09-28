package com.weeth.domain.club.application.usecase.query

import com.weeth.domain.attendance.domain.repository.AttendanceReader
import com.weeth.domain.attendance.domain.repository.MemberAttendanceCount
import com.weeth.domain.board.domain.repository.PostReader
import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.club.application.dto.request.ClubMemberSort
import com.weeth.domain.club.application.exception.ClubMemberNotFoundException
import com.weeth.domain.club.application.exception.ClubMemberNotInClubException
import com.weeth.domain.club.application.exception.MemberNotActiveException
import com.weeth.domain.club.application.mapper.ClubMapper
import com.weeth.domain.club.application.mapper.ClubPositionOptionMapper
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberCardinalReader
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.repository.ClubPositionOptionReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubPositionOptionTestFixture
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
import org.springframework.data.domain.SliceImpl
import org.springframework.test.util.ReflectionTestUtils

class GetClubMemberQueryServiceTest :
    DescribeSpec({
        val clubMemberReader = mockk<ClubMemberReader>()
        val clubMemberCardinalReader = mockk<ClubMemberCardinalReader>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>()
        val fileAccessUrlPort = mockk<FileAccessUrlPort>()
        val userReader = mockk<UserReader>()
        val penaltyReader = mockk<PenaltyReader>()
        val postReader = mockk<PostReader>()
        val clubPositionOptionReader = mockk<ClubPositionOptionReader>()
        val attendanceReader = mockk<AttendanceReader>()
        val clubMapper = ClubMapper(fileAccessUrlPort)
        val clubPositionOptionMapper = ClubPositionOptionMapper()

        val service =
            GetClubMemberQueryService(
                clubMemberReader = clubMemberReader,
                clubMemberCardinalReader = clubMemberCardinalReader,
                clubMemberPolicy = clubMemberPolicy,
                clubPermissionPolicy = clubPermissionPolicy,
                clubMapper = clubMapper,
                clubPositionOptionMapper = clubPositionOptionMapper,
                clubPositionOptionReader = clubPositionOptionReader,
                userReader = userReader,
                penaltyReader = penaltyReader,
                postReader = postReader,
                attendanceReader = attendanceReader,
            )

        beforeTest {
            clearMocks(
                clubMemberReader,
                clubMemberCardinalReader,
                clubMemberPolicy,
                clubPermissionPolicy,
                userReader,
                penaltyReader,
                postReader,
                clubPositionOptionReader,
                attendanceReader,
            )
            every { attendanceReader.countByClubIdAndMemberIdsAndCardinal(any(), any(), any()) } returns emptyList()
        }

        describe("기수별 관리자 출석 통계") {
            it("페이지 멤버 통계를 한 번에 집계하며 기록 없는 멤버는 누적값 대신 0을 반환한다") {
                val club = ClubTestFixture.createClub(id = 1L)
                val admin = ClubMemberTestFixture.createAdminMember(club = club)
                val member = ClubMemberTestFixture.createActiveMember(id = 10L, club = club)
                val emptyMember = ClubMemberTestFixture.createActiveMember(id = 11L, club = club)
                repeat(9) {
                    member.attend()
                    emptyMember.attend()
                }
                val members = listOf(member, emptyMember)

                every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns admin
                every { clubMemberReader.findAdminMembers(1L, 7, null, null, "CARDINAL_DESC", any()) } returns
                    PageImpl(members, PageRequest.of(0, 20), 2)
                every { clubMemberCardinalReader.findAllByClubMembers(members) } returns emptyList()
                every { penaltyReader.findByClubMemberIds(any()) } returns emptyList()
                val count = mockk<MemberAttendanceCount>()
                every { count.clubMemberId } returns 10L
                every { count.attendanceCount } returns 1L
                every { count.absenceCount } returns 2L
                every { attendanceReader.countByClubIdAndMemberIdsAndCardinal(1L, listOf(10L, 11L), 7) } returns
                    listOf(count)

                val result =
                    service.findClubMembersForAdmin(
                        clubId = 1L,
                        userId = 99L,
                        page = 0,
                        size = 20,
                        keyword = null,
                        cardinalNumber = 7,
                        memberRole = null,
                        sort = ClubMemberSort.CARDINAL_DESC,
                    )

                result.content[0].attendanceCount shouldBe 1
                result.content[0].absenceCount shouldBe 2
                result.content[0].attendanceRate shouldBe 33
                result.content[1].attendanceCount shouldBe 0
                result.content[1].absenceCount shouldBe 0
                result.content[1].attendanceRate shouldBe 0
                // 기수 조회는 멤버 누적 카운터를 변경하지 않는다
                member.attendanceStats.attendanceCount shouldBe 9
                emptyMember.attendanceStats.attendanceCount shouldBe 9
                verify(exactly = 1) { attendanceReader.countByClubIdAndMemberIdsAndCardinal(any(), any(), any()) }
            }

            it("기수를 지정하지 않으면 누적 카운터를 반환하고 집계 쿼리를 실행하지 않는다") {
                val club = ClubTestFixture.createClub(id = 1L)
                val admin = ClubMemberTestFixture.createAdminMember(club = club)
                val member = ClubMemberTestFixture.createActiveMember(id = 10L, club = club)
                repeat(3) { member.attend() }
                member.absent()

                every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns admin
                every { clubMemberReader.findAdminMembers(1L, null, null, null, "CARDINAL_DESC", any()) } returns
                    PageImpl(listOf(member), PageRequest.of(0, 20), 1)
                every { clubMemberCardinalReader.findAllByClubMembers(listOf(member)) } returns emptyList()
                every { penaltyReader.findByClubMemberIds(any()) } returns emptyList()

                val result =
                    service.findClubMembersForAdmin(
                        clubId = 1L,
                        userId = 99L,
                        page = 0,
                        size = 20,
                        keyword = null,
                        cardinalNumber = null,
                        memberRole = null,
                        sort = ClubMemberSort.CARDINAL_DESC,
                    )

                result.content[0].attendanceCount shouldBe 3
                result.content[0].absenceCount shouldBe 1
                result.content[0].attendanceRate shouldBe 75
                verify(exactly = 0) { attendanceReader.countByClubIdAndMemberIdsAndCardinal(any(), any(), any()) }
            }

            it("페이지가 비어 있으면 기수를 지정해도 집계 쿼리를 실행하지 않는다") {
                val club = ClubTestFixture.createClub(id = 1L)
                val admin = ClubMemberTestFixture.createAdminMember(club = club)

                every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns admin
                every { clubMemberReader.findAdminMembers(1L, 7, null, null, "CARDINAL_DESC", any()) } returns
                    PageImpl(emptyList(), PageRequest.of(0, 20), 0)
                every { clubMemberCardinalReader.findAllByClubMembers(emptyList()) } returns emptyList()

                val result =
                    service.findClubMembersForAdmin(
                        clubId = 1L,
                        userId = 99L,
                        page = 0,
                        size = 20,
                        keyword = null,
                        cardinalNumber = 7,
                        memberRole = null,
                        sort = ClubMemberSort.CARDINAL_DESC,
                    )

                result.content.shouldBeEmpty()
                verify(exactly = 0) { attendanceReader.countByClubIdAndMemberIdsAndCardinal(any(), any(), any()) }
            }

            it("검색도 기수 지정 시 해당 기수 집계를 사용한다") {
                val club = ClubTestFixture.createClub(id = 1L)
                val admin = ClubMemberTestFixture.createAdminMember(club = club)
                val member = ClubMemberTestFixture.createActiveMember(id = 10L, club = club)
                repeat(9) { member.attend() }

                every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns admin
                every { clubMemberReader.findAdminMembers(1L, 7, null, "홍길동", "CARDINAL_DESC", any()) } returns
                    PageImpl(listOf(member), PageRequest.of(0, 50), 1)
                every { clubMemberCardinalReader.findAllByClubMembers(listOf(member)) } returns emptyList()
                every { penaltyReader.findByClubMemberIds(any()) } returns emptyList()
                val count = mockk<MemberAttendanceCount>()
                every { count.clubMemberId } returns 10L
                every { count.attendanceCount } returns 2L
                every { count.absenceCount } returns 2L
                every { attendanceReader.countByClubIdAndMemberIdsAndCardinal(1L, listOf(10L), 7) } returns
                    listOf(count)

                val result =
                    service.searchClubMembers(
                        clubId = 1L,
                        userId = 99L,
                        keyword = "홍길동",
                        cardinalNumber = 7,
                    )

                result shouldHaveSize 1
                result[0].attendanceCount shouldBe 2
                result[0].absenceCount shouldBe 2
                result[0].attendanceRate shouldBe 50
                verify(exactly = 1) { attendanceReader.countByClubIdAndMemberIdsAndCardinal(1L, listOf(10L), 7) }
            }
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
                    clubMemberReader.findAdminMembers(1L, null, null, "홍길동", "CARDINAL_DESC", any())
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
                    clubMemberReader.findAdminMembers(1L, null, null, "홍길동", "CARDINAL_DESC", any())
                }
            }

            it("특정 기수에서만 멤버를 검색한다") {
                val club = ClubTestFixture.createClub()
                val admin = ClubTestFixture.createClubMember(club = club, memberRole = MemberRole.ADMIN)
                val member = ClubTestFixture.createClubMember(club = club)

                every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns admin
                every {
                    clubMemberReader.findAdminMembers(1L, 7, null, "김", "CARDINAL_DESC", any())
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
                    clubMemberReader.findAdminMembers(1L, null, null, "존재하지않음", "CARDINAL_DESC", any())
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
                    clubMemberReader.findAdminMembers(1L, null, null, "김", "CARDINAL_DESC", any())
                } returns PageImpl(emptyList(), PageRequest.of(0, 50), 0)
                every { clubMemberCardinalReader.findAllByClubMembers(emptyList()) } returns emptyList()

                service.searchClubMembers(
                    clubId = 1L,
                    userId = 99L,
                    keyword = "김",
                    cardinalNumber = null,
                )

                verify(exactly = 1) {
                    clubMemberReader.findAdminMembers(1L, null, null, "김", "CARDINAL_DESC", any())
                }
            }

            it("keyword가 역할 라벨과 정확히 일치하면 이름 검색 대신 역할로 필터링한다") {
                val club = ClubTestFixture.createClub()
                val admin = ClubTestFixture.createClubMember(club = club, memberRole = MemberRole.ADMIN)
                val member = ClubTestFixture.createClubMember(club = club, memberRole = MemberRole.ADMIN)

                every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns admin
                every {
                    clubMemberReader.findAdminMembers(1L, null, MemberRole.ADMIN, null, "CARDINAL_DESC", any())
                } returns PageImpl(listOf(member), PageRequest.of(0, 50), 1)
                every { clubMemberCardinalReader.findAllByClubMembers(listOf(member)) } returns emptyList()
                every { penaltyReader.findByClubMemberIds(any()) } returns emptyList()

                val result =
                    service.searchClubMembers(
                        clubId = 1L,
                        userId = 99L,
                        keyword = "운영진",
                        cardinalNumber = null,
                    )

                result shouldHaveSize 1
                verify(exactly = 1) {
                    clubMemberReader.findAdminMembers(1L, null, MemberRole.ADMIN, null, "CARDINAL_DESC", any())
                }
            }

            it("keyword가 역할 라벨과 부분일치만 해도 이름 검색으로 취급한다") {
                val club = ClubTestFixture.createClub()
                val admin = ClubTestFixture.createClubMember(club = club, memberRole = MemberRole.ADMIN)
                val member = ClubTestFixture.createClubMember(club = club)

                every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns admin
                every {
                    clubMemberReader.findAdminMembers(1L, null, null, "김리더", "CARDINAL_DESC", any())
                } returns PageImpl(listOf(member), PageRequest.of(0, 50), 1)
                every { clubMemberCardinalReader.findAllByClubMembers(listOf(member)) } returns emptyList()
                every { penaltyReader.findByClubMemberIds(any()) } returns emptyList()

                val result =
                    service.searchClubMembers(
                        clubId = 1L,
                        userId = 99L,
                        keyword = "김리더",
                        cardinalNumber = null,
                    )

                result shouldHaveSize 1
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
                        clubMemberReader.findAdminMembers(1L, null, null, null, "CARDINAL_DESC", any())
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
                            memberRole = null,
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
                    response.warningCount shouldBe null
                    response.cardinals shouldBe listOf(6, 7)
                    response.joinedAt shouldBe member.createdAt
                    verify(exactly = 1) { clubPermissionPolicy.requireAdmin(1L, 99L) }
                    verify(exactly = 1) { clubMemberCardinalReader.findAllByClubMembers(listOf(member)) }
                }

                it("동아리 경고 기능이 활성화된 경우 경고 횟수를 포함한다") {
                    val club = ClubTestFixture.createClub()
                    ReflectionTestUtils.setField(club, "warningEnabled", true)
                    val admin = ClubTestFixture.createClubMember(club = club, memberRole = MemberRole.ADMIN)
                    val member =
                        ClubTestFixture.createClubMember(club = club, user = UserTestFixture.createActiveUser1(1L))
                    ReflectionTestUtils.setField(member, "warningCount", 1)

                    every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns admin
                    every {
                        clubMemberReader.findAdminMembers(1L, null, null, null, "CARDINAL_DESC", any())
                    } returns PageImpl(listOf(member), PageRequest.of(0, 20), 1)
                    every { clubMemberCardinalReader.findAllByClubMembers(listOf(member)) } returns emptyList()
                    every { penaltyReader.findByClubMemberIds(any()) } returns emptyList()

                    val result =
                        service.findClubMembersForAdmin(
                            clubId = 1L,
                            userId = 99L,
                            page = 0,
                            size = 20,
                            keyword = null,
                            cardinalNumber = null,
                            memberRole = null,
                            sort = ClubMemberSort.CARDINAL_DESC,
                        )

                    result.content.first().warningCount shouldBe 1
                }

                it("검색어 공백을 제거하고 기수 필터·정렬을 그대로 리포지토리에 전달한다") {
                    val club = ClubTestFixture.createClub()
                    val admin = ClubTestFixture.createClubMember(club = club, memberRole = MemberRole.ADMIN)

                    every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns admin
                    every {
                        clubMemberReader.findAdminMembers(1L, 7, null, "홍길동", "NAME_ASC", any())
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
                            memberRole = null,
                            sort = ClubMemberSort.NAME_ASC,
                        )

                    result.content.shouldBeEmpty()
                    verify(exactly = 1) {
                        clubMemberReader.findAdminMembers(1L, 7, null, "홍길동", "NAME_ASC", any())
                    }
                }

                it("역할 필터를 그대로 리포지토리에 전달한다") {
                    val club = ClubTestFixture.createClub()
                    val admin = ClubTestFixture.createClubMember(club = club, memberRole = MemberRole.ADMIN)

                    every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns admin
                    every {
                        clubMemberReader.findAdminMembers(1L, null, MemberRole.ADMIN, null, "CARDINAL_DESC", any())
                    } returns PageImpl(emptyList(), PageRequest.of(0, 20), 0)
                    every { clubMemberCardinalReader.findAllByClubMembers(emptyList()) } returns emptyList()

                    service.findClubMembersForAdmin(
                        clubId = 1L,
                        userId = 99L,
                        page = 0,
                        size = 20,
                        keyword = null,
                        cardinalNumber = null,
                        memberRole = MemberRole.ADMIN,
                        sort = ClubMemberSort.CARDINAL_DESC,
                    )

                    verify(exactly = 1) {
                        clubMemberReader.findAdminMembers(1L, null, MemberRole.ADMIN, null, "CARDINAL_DESC", any())
                    }
                }

                it("페이지 크기를 1~100으로 보정한다") {
                    val club = ClubTestFixture.createClub()
                    val admin = ClubTestFixture.createClubMember(club = club, memberRole = MemberRole.ADMIN)
                    val pageableSlot = slot<Pageable>()

                    every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns admin
                    every {
                        clubMemberReader.findAdminMembers(1L, null, null, null, any(), capture(pageableSlot))
                    } returns PageImpl(emptyList(), PageRequest.of(0, 100), 0)
                    every { clubMemberCardinalReader.findAllByClubMembers(emptyList()) } returns emptyList()

                    service.findClubMembersForAdmin(
                        clubId = 1L,
                        userId = 99L,
                        page = -5,
                        size = 500,
                        keyword = null,
                        cardinalNumber = null,
                        memberRole = null,
                        sort = ClubMemberSort.CARDINAL_DESC,
                    )

                    pageableSlot.captured.pageNumber shouldBe 0
                    pageableSlot.captured.pageSize shouldBe 100
                }

                it("포지션이 미지정인 멤버는 position이 null이다") {
                    val club = ClubTestFixture.createClub()
                    val admin = ClubTestFixture.createClubMember(club = club, memberRole = MemberRole.ADMIN)
                    val member = ClubTestFixture.createClubMember(club = club)

                    every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns admin
                    every {
                        clubMemberReader.findAdminMembers(1L, null, null, null, "CARDINAL_DESC", any())
                    } returns PageImpl(listOf(member), PageRequest.of(0, 20), 1)
                    every { clubMemberCardinalReader.findAllByClubMembers(listOf(member)) } returns emptyList()
                    every { penaltyReader.findByClubMemberIds(any()) } returns emptyList()

                    val result =
                        service.findClubMembersForAdmin(
                            clubId = 1L,
                            userId = 99L,
                            page = 0,
                            size = 20,
                            keyword = null,
                            cardinalNumber = null,
                            memberRole = null,
                            sort = ClubMemberSort.CARDINAL_DESC,
                        )

                    result.content.first().position shouldBe null
                    verify(exactly = 0) { clubPositionOptionReader.findAllByIdIn(any()) }
                }

                it("포지션이 지정된 멤버들을 일괄 조회(findAllByIdIn)로 매핑하고 행 단위로 역참조하지 않는다") {
                    val club = ClubTestFixture.createClub(id = 1L)
                    val admin = ClubTestFixture.createClubMember(club = club, memberRole = MemberRole.ADMIN)
                    val option = ClubPositionOptionTestFixture.createOption(id = 100L, club = club, name = "백엔드")
                    val member1 =
                        ClubMemberTestFixture.createActiveMember(id = 10L, club = club).also {
                            it.assignPosition(option)
                        }
                    val member2 =
                        ClubMemberTestFixture.createActiveMember(id = 11L, club = club).also {
                            it.assignPosition(option)
                        }

                    every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns admin
                    every {
                        clubMemberReader.findAdminMembers(1L, null, null, null, "CARDINAL_DESC", any())
                    } returns PageImpl(listOf(member1, member2), PageRequest.of(0, 20), 2)
                    every {
                        clubMemberCardinalReader.findAllByClubMembers(listOf(member1, member2))
                    } returns emptyList()
                    every { penaltyReader.findByClubMemberIds(any()) } returns emptyList()
                    every { clubPositionOptionReader.findAllByIdIn(listOf(100L)) } returns listOf(option)

                    val result =
                        service.findClubMembersForAdmin(
                            clubId = 1L,
                            userId = 99L,
                            page = 0,
                            size = 20,
                            keyword = null,
                            cardinalNumber = null,
                            memberRole = null,
                            sort = ClubMemberSort.CARDINAL_DESC,
                        )

                    result.content.map { it.position?.id } shouldBe listOf(100L, 100L)
                    result.content
                        .first()
                        .position
                        ?.name shouldBe "백엔드"
                    // 멤버가 2명이어도 findAllByIdIn은 distinct id로 1회만 호출된다 (N+1 회피)
                    verify(exactly = 1) { clubPositionOptionReader.findAllByIdIn(listOf(100L)) }
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
                    result.warningCount shouldBe null
                }
            }

            context("동아리 경고 기능이 활성화된 경우") {
                it("경고 횟수를 포함한다") {
                    val warningClub = ClubTestFixture.createClub(id = 1L)
                    ReflectionTestUtils.setField(warningClub, "warningEnabled", true)
                    val warningAdmin =
                        ClubTestFixture.createClubMember(
                            club = warningClub,
                            memberRole = MemberRole.ADMIN,
                        )
                    val member = ClubMemberTestFixture.createActiveMember(id = 5L, club = warningClub)
                    ReflectionTestUtils.setField(member, "warningCount", 1)

                    every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns warningAdmin
                    every { clubMemberReader.findAdminMemberDetail(5L) } returns member
                    every { clubMemberCardinalReader.findAllByClubMember(member) } returns emptyList()

                    val result = service.findClubMemberDetailForAdmin(clubId = 1L, userId = 99L, clubMemberId = 5L)

                    result.warningCount shouldBe 1
                }
            }

            context("포지션이 지정된 멤버를 조회하는 경우") {
                it("position을 직접 resolve해서 반환한다") {
                    val option = ClubPositionOptionTestFixture.createOption(id = 100L, club = club, name = "백엔드")
                    val member =
                        ClubMemberTestFixture.createActiveMember(id = 5L, club = club).also {
                            it.assignPosition(option)
                        }

                    every { clubPermissionPolicy.requireAdmin(1L, 99L) } returns admin
                    every { clubMemberReader.findAdminMemberDetail(5L) } returns member
                    every { clubMemberCardinalReader.findAllByClubMember(member) } returns emptyList()

                    val result = service.findClubMemberDetailForAdmin(clubId = 1L, userId = 99L, clubMemberId = 5L)

                    result.position?.id shouldBe 100L
                    result.position?.name shouldBe "백엔드"
                    // 단건 조회는 페이지 일괄 조회 대상이 아니므로 findAllByIdIn을 쓰지 않는다
                    verify(exactly = 0) { clubPositionOptionReader.findAllByIdIn(any()) }
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

        describe("findMemberDetail") {
            val club = ClubTestFixture.createClub(id = 1L)
            val clubId = 1L
            val userId = 99L
            val clubMemberId = 10L

            context("활성 멤버 상세를 조회하는 경우") {
                it("ClubMemberDetailResponse를 반환한다") {
                    val caller = ClubMemberTestFixture.createActiveMember(club = club)
                    val targetUser = UserTestFixture.createActiveUser1(1L)
                    val targetMember =
                        ClubMemberTestFixture.createActiveMember(
                            id = clubMemberId,
                            club = club,
                            user = targetUser,
                        )
                    val cardinal = Cardinal.create(club = club, cardinalNumber = 7)
                    val memberCardinal = ClubMemberCardinal.create(targetMember, cardinal)

                    every { clubMemberPolicy.getActiveMember(clubId, userId) } returns caller
                    every { clubMemberReader.findPublicMemberDetail(clubId, clubMemberId) } returns targetMember
                    every { clubMemberCardinalReader.findAllByClubMember(targetMember) } returns listOf(memberCardinal)
                    every { postReader.countActiveByClubMemberIds(listOf(clubMemberId)) } returns 5L

                    val result =
                        service.findMemberDetail(
                            clubId = clubId,
                            userId = userId,
                            clubMemberId = clubMemberId,
                        )

                    result.clubMemberId shouldBe clubMemberId
                    result.name shouldBe targetUser.name
                    result.memberRole shouldBe MemberRole.USER
                    result.cardinals shouldBe listOf(7)
                    result.postCount shouldBe 5L
                    result.email shouldBe targetUser.emailValue
                    result.position shouldBe null
                }

                it("포지션이 지정된 멤버는 응답에 position이 포함된다") {
                    val caller = ClubMemberTestFixture.createActiveMember(club = club)
                    val option = ClubPositionOptionTestFixture.createOption(id = 100L, club = club, name = "백엔드")
                    val targetMember =
                        ClubMemberTestFixture
                            .createActiveMember(
                                id = clubMemberId,
                                club = club,
                                user = UserTestFixture.createActiveUser1(1L),
                            ).also { it.assignPosition(option) }

                    every { clubMemberPolicy.getActiveMember(clubId, userId) } returns caller
                    every { clubMemberReader.findPublicMemberDetail(clubId, clubMemberId) } returns targetMember
                    every { clubMemberCardinalReader.findAllByClubMember(targetMember) } returns emptyList()
                    every { postReader.countActiveByClubMemberIds(listOf(clubMemberId)) } returns 0L

                    val result =
                        service.findMemberDetail(
                            clubId = clubId,
                            userId = userId,
                            clubMemberId = clubMemberId,
                        )

                    result.position?.name shouldBe "백엔드"
                }
            }

            context("존재하지 않는 멤버를 조회하는 경우") {
                it("ClubMemberNotFoundException을 던진다") {
                    val caller = ClubMemberTestFixture.createActiveMember(club = club)

                    every { clubMemberPolicy.getActiveMember(clubId, userId) } returns caller
                    every { clubMemberReader.findPublicMemberDetail(clubId, clubMemberId) } returns null

                    shouldThrow<ClubMemberNotFoundException> {
                        service.findMemberDetail(
                            clubId = clubId,
                            userId = userId,
                            clubMemberId = clubMemberId,
                        )
                    }
                }
            }

            context("비활성 멤버가 조회하는 경우") {
                it("MemberNotActiveException을 던진다") {
                    every { clubMemberPolicy.getActiveMember(clubId, userId) } throws MemberNotActiveException()

                    shouldThrow<MemberNotActiveException> {
                        service.findMemberDetail(
                            clubId = clubId,
                            userId = userId,
                            clubMemberId = clubMemberId,
                        )
                    }
                }
            }
        }

        describe("findPublicMembers") {
            val club = ClubTestFixture.createClub(id = 1L)
            val clubId = 1L
            val userId = 99L

            context("활성 멤버가 조회하는 경우") {
                it("멤버 목록을 SliceResponse로 반환한다") {
                    val caller = ClubMemberTestFixture.createActiveMember(club = club)
                    val member =
                        ClubMemberTestFixture.createActiveMember(
                            id = 10L,
                            club = club,
                            user = UserTestFixture.createActiveUser1(1L),
                        )
                    val cardinal = Cardinal.create(club = club, cardinalNumber = 7)
                    val memberCardinal = ClubMemberCardinal.create(member, cardinal)

                    every { clubMemberPolicy.getActiveMember(clubId, userId) } returns caller
                    every {
                        clubMemberReader.findPublicMembers(clubId, null, null, null, null, any())
                    } returns SliceImpl(listOf(member), PageRequest.of(0, 20), false)
                    every { clubMemberCardinalReader.findAllByClubMembers(listOf(member)) } returns
                        listOf(memberCardinal)

                    val result =
                        service.findPublicMembers(
                            clubId = clubId,
                            userId = userId,
                            cardinalNumber = null,
                            memberRole = null,
                            keyword = null,
                            positionOptionId = null,
                            page = 0,
                            size = 20,
                        )

                    result.content shouldHaveSize 1
                    result.hasNext shouldBe false
                    val response = result.content.first()
                    response.name shouldBe member.user.name
                    response.memberRole shouldBe MemberRole.USER
                    response.cardinals shouldBe listOf(7)
                    verify(exactly = 1) { clubMemberPolicy.getActiveMember(clubId, userId) }
                }

                it("기수 필터를 Repository에 전달한다") {
                    val caller = ClubMemberTestFixture.createActiveMember(club = club)
                    val pageableSlot = slot<Pageable>()

                    every { clubMemberPolicy.getActiveMember(clubId, userId) } returns caller
                    every {
                        clubMemberReader.findPublicMembers(clubId, 7, null, null, null, capture(pageableSlot))
                    } returns SliceImpl(emptyList(), PageRequest.of(0, 20), false)
                    every { clubMemberCardinalReader.findAllByClubMembers(emptyList()) } returns emptyList()

                    service.findPublicMembers(
                        clubId = clubId,
                        userId = userId,
                        cardinalNumber = 7,
                        memberRole = null,
                        keyword = null,
                        positionOptionId = null,
                        page = 0,
                        size = 20,
                    )

                    verify(exactly = 1) { clubMemberReader.findPublicMembers(clubId, 7, null, null, null, any()) }
                }

                it("역할 필터를 Repository에 전달한다") {
                    val caller = ClubMemberTestFixture.createActiveMember(club = club)

                    every { clubMemberPolicy.getActiveMember(clubId, userId) } returns caller
                    every {
                        clubMemberReader.findPublicMembers(clubId, null, MemberRole.ADMIN, null, null, any())
                    } returns SliceImpl(emptyList(), PageRequest.of(0, 20), false)
                    every { clubMemberCardinalReader.findAllByClubMembers(emptyList()) } returns emptyList()

                    service.findPublicMembers(
                        clubId = clubId,
                        userId = userId,
                        cardinalNumber = null,
                        memberRole = MemberRole.ADMIN,
                        keyword = null,
                        positionOptionId = null,
                        page = 0,
                        size = 20,
                    )

                    verify(
                        exactly = 1,
                    ) { clubMemberReader.findPublicMembers(clubId, null, MemberRole.ADMIN, null, null, any()) }
                }

                it("포지션 필터를 Repository에 전달한다") {
                    val caller = ClubMemberTestFixture.createActiveMember(club = club)

                    every { clubMemberPolicy.getActiveMember(clubId, userId) } returns caller
                    every {
                        clubMemberReader.findPublicMembers(clubId, null, null, null, 100L, any())
                    } returns SliceImpl(emptyList(), PageRequest.of(0, 20), false)
                    every { clubMemberCardinalReader.findAllByClubMembers(emptyList()) } returns emptyList()

                    service.findPublicMembers(
                        clubId = clubId,
                        userId = userId,
                        cardinalNumber = null,
                        memberRole = null,
                        keyword = null,
                        positionOptionId = 100L,
                        page = 0,
                        size = 20,
                    )

                    verify(
                        exactly = 1,
                    ) { clubMemberReader.findPublicMembers(clubId, null, null, null, 100L, any()) }
                }

                it("포지션이 지정된 멤버는 응답에 position이 포함된다") {
                    val caller = ClubMemberTestFixture.createActiveMember(club = club)
                    val option = ClubPositionOptionTestFixture.createOption(id = 100L, club = club, name = "백엔드")
                    val member =
                        ClubMemberTestFixture
                            .createActiveMember(
                                id = 10L,
                                club = club,
                                user = UserTestFixture.createActiveUser1(1L),
                            ).also { it.assignPosition(option) }

                    every { clubMemberPolicy.getActiveMember(clubId, userId) } returns caller
                    every {
                        clubMemberReader.findPublicMembers(clubId, null, null, null, null, any())
                    } returns SliceImpl(listOf(member), PageRequest.of(0, 20), false)
                    every { clubMemberCardinalReader.findAllByClubMembers(listOf(member)) } returns emptyList()
                    every { clubPositionOptionReader.findAllByIdIn(listOf(100L)) } returns listOf(option)

                    val result =
                        service.findPublicMembers(
                            clubId = clubId,
                            userId = userId,
                            cardinalNumber = null,
                            memberRole = null,
                            keyword = null,
                            positionOptionId = null,
                            page = 0,
                            size = 20,
                        )

                    result.content
                        .first()
                        .position
                        ?.name shouldBe "백엔드"
                }

                it("조회 결과가 없으면 빈 SliceResponse를 반환한다") {
                    val caller = ClubMemberTestFixture.createActiveMember(club = club)

                    every { clubMemberPolicy.getActiveMember(clubId, userId) } returns caller
                    every {
                        clubMemberReader.findPublicMembers(clubId, null, null, null, null, any())
                    } returns SliceImpl(emptyList(), PageRequest.of(0, 20), false)
                    every { clubMemberCardinalReader.findAllByClubMembers(emptyList()) } returns emptyList()

                    val result =
                        service.findPublicMembers(
                            clubId = clubId,
                            userId = userId,
                            cardinalNumber = null,
                            memberRole = null,
                            keyword = null,
                            positionOptionId = null,
                            page = 0,
                            size = 20,
                        )

                    result.content.shouldBeEmpty()
                    result.hasNext shouldBe false
                }
            }

            context("비활성 멤버가 접근하는 경우") {
                it("MemberNotActiveException을 던진다") {
                    every { clubMemberPolicy.getActiveMember(clubId, userId) } throws MemberNotActiveException()

                    shouldThrow<MemberNotActiveException> {
                        service.findPublicMembers(
                            clubId = clubId,
                            userId = userId,
                            cardinalNumber = null,
                            memberRole = null,
                            keyword = null,
                            positionOptionId = null,
                            page = 0,
                            size = 20,
                        )
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
