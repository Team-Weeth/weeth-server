package com.weeth.domain.club.presentation

import com.weeth.domain.club.application.dto.response.ClubMemberResponse
import com.weeth.domain.club.application.usecase.command.AdminClubMemberUseCase
import com.weeth.domain.club.application.usecase.command.ManageClubUseCase
import com.weeth.domain.club.application.usecase.query.GetClubMemberQueryService
import com.weeth.domain.club.application.usecase.query.GetClubQueryService
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.global.common.response.PageResponse
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class ClubAdminControllerTest :
    DescribeSpec({
        val manageClubUseCase = mockk<ManageClubUseCase>()
        val adminClubMemberUseCase = mockk<AdminClubMemberUseCase>()
        val getClubQueryService = mockk<GetClubQueryService>()
        val getClubMemberQueryService = mockk<GetClubMemberQueryService>()

        val controller =
            ClubAdminController(
                manageClubUseCase = manageClubUseCase,
                adminClubMemberUseCase = adminClubMemberUseCase,
                getClubQueryService = getClubQueryService,
                getClubMemberQueryService = getClubMemberQueryService,
            )

        val clubId = 1L
        val userId = 99L

        beforeTest {
            clearMocks(
                manageClubUseCase,
                adminClubMemberUseCase,
                getClubQueryService,
                getClubMemberQueryService,
            )
        }

        describe("searchClubMembers") {
            it("멤버 검색 성공 코드를 반환한다") {
                val member =
                    ClubMemberResponse(
                        userId = 1L,
                        clubMemberId = 100L,
                        name = "홍길동",
                        email = "hong@example.com",
                        tel = "01012345678",
                        school = "가천대학교",
                        department = "컴퓨터공학과",
                        studentId = "20201234",
                        cardinals = listOf(7, 6),
                        memberStatus = MemberStatus.ACTIVE,
                        memberRole = MemberRole.USER,
                        attendanceCount = 10,
                        absenceCount = 2,
                        attendanceRate = 83,
                        penaltyCount = 1,
                        lastPenaltyAt = LocalDateTime.of(2026, 8, 24, 10, 0),
                        profileImageUrl = "https://example.com/profile.jpg",
                        bio = "안녕하세요",
                        joinedAt = LocalDateTime.of(2026, 3, 1, 10, 0),
                    )
                val pageImpl = PageImpl(listOf(member), PageRequest.of(0, 20), 1)
                val pageResponse = PageResponse.from(pageImpl)

                every {
                    getClubMemberQueryService.searchClubMembers(
                        clubId = clubId,
                        userId = userId,
                        keyword = "홍길동",
                        cardinalNumber = null,
                        page = 0,
                        size = 20,
                    )
                } returns pageResponse

                val response =
                    controller.searchClubMembers(
                        userId = userId,
                        clubId = clubId,
                        keyword = "홍길동",
                        cardinalNumber = null,
                        page = 0,
                        size = 20,
                    )

                response.code shouldBe ClubResponseCode.MEMBER_FIND_ALL_SUCCESS.code
                response.data?.content?.size shouldBe 1
                response.data
                    ?.content
                    ?.first()
                    ?.name shouldBe "홍길동"
            }

            it("특정 기수로 검색할 수 있다") {
                val member =
                    ClubMemberResponse(
                        userId = 2L,
                        clubMemberId = 101L,
                        name = "김지원",
                        email = "kim@example.com",
                        tel = "01087654321",
                        school = "가천대학교",
                        department = "컴퓨터공학과",
                        studentId = "20215678",
                        cardinals = listOf(5),
                        memberStatus = MemberStatus.ACTIVE,
                        memberRole = MemberRole.ADMIN,
                        attendanceCount = 15,
                        absenceCount = 0,
                        attendanceRate = 100,
                        penaltyCount = 0,
                        lastPenaltyAt = null,
                        profileImageUrl = null,
                        bio = null,
                        joinedAt = LocalDateTime.of(2024, 3, 1, 10, 0),
                    )
                val pageImpl = PageImpl(listOf(member), PageRequest.of(0, 20), 1)
                val pageResponse = PageResponse.from(pageImpl)

                every {
                    getClubMemberQueryService.searchClubMembers(
                        clubId = clubId,
                        userId = userId,
                        keyword = "김",
                        cardinalNumber = 5,
                        page = 0,
                        size = 20,
                    )
                } returns pageResponse

                val response =
                    controller.searchClubMembers(
                        userId = userId,
                        clubId = clubId,
                        keyword = "김",
                        cardinalNumber = 5,
                        page = 0,
                        size = 20,
                    )

                response.code shouldBe ClubResponseCode.MEMBER_FIND_ALL_SUCCESS.code
                response.data
                    ?.content
                    ?.first()
                    ?.cardinals shouldBe listOf(5)
            }

            it("검색 결과가 없을 수 있다") {
                val pageImpl = PageImpl(emptyList<ClubMemberResponse>(), PageRequest.of(0, 20), 0)
                val pageResponse = PageResponse.from(pageImpl)

                every {
                    getClubMemberQueryService.searchClubMembers(
                        clubId = clubId,
                        userId = userId,
                        keyword = "존재하지않음",
                        cardinalNumber = null,
                        page = 0,
                        size = 20,
                    )
                } returns pageResponse

                val response =
                    controller.searchClubMembers(
                        userId = userId,
                        clubId = clubId,
                        keyword = "존재하지않음",
                        cardinalNumber = null,
                        page = 0,
                        size = 20,
                    )

                response.code shouldBe ClubResponseCode.MEMBER_FIND_ALL_SUCCESS.code
                response.data?.content?.size shouldBe 0
            }

            it("페이지네이션을 지원한다") {
                val members =
                    (1..10).map {
                        ClubMemberResponse(
                            userId = it.toLong(),
                            clubMemberId = (100 + it).toLong(),
                            name = "멤버$it",
                            email = "member$it@example.com",
                            tel = null,
                            school = null,
                            department = null,
                            studentId = null,
                            cardinals = emptyList(),
                            memberStatus = MemberStatus.ACTIVE,
                            memberRole = MemberRole.USER,
                            attendanceCount = 0,
                            absenceCount = 0,
                            attendanceRate = 0,
                            penaltyCount = 0,
                            lastPenaltyAt = null,
                            profileImageUrl = null,
                            bio = null,
                            joinedAt = LocalDateTime.now(),
                        )
                    }
                val pageImpl = PageImpl(members, PageRequest.of(0, 10), 25)
                val pageResponse = PageResponse.from(pageImpl)

                every {
                    getClubMemberQueryService.searchClubMembers(
                        clubId = clubId,
                        userId = userId,
                        keyword = "멤버",
                        cardinalNumber = null,
                        page = 0,
                        size = 10,
                    )
                } returns pageResponse

                val response =
                    controller.searchClubMembers(
                        userId = userId,
                        clubId = clubId,
                        keyword = "멤버",
                        cardinalNumber = null,
                        page = 0,
                        size = 10,
                    )

                response.code shouldBe ClubResponseCode.MEMBER_FIND_ALL_SUCCESS.code
                response.data?.content?.size shouldBe 10
                response.data?.totalElements shouldBe 25
            }
        }
    })
