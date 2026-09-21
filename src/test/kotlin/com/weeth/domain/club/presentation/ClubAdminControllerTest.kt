package com.weeth.domain.club.presentation

import com.weeth.domain.club.application.dto.request.ClubMemberPositionUpdateRequest
import com.weeth.domain.club.application.dto.request.ClubPositionOptionRequest
import com.weeth.domain.club.application.dto.request.SaveClubPositionOptionsRequest
import com.weeth.domain.club.application.dto.response.ClubMemberResponse
import com.weeth.domain.club.application.dto.response.ClubPositionOptionResponse
import com.weeth.domain.club.application.usecase.command.AdminClubMemberUseCase
import com.weeth.domain.club.application.usecase.command.ManageClubPositionOptionUseCase
import com.weeth.domain.club.application.usecase.command.ManageClubUseCase
import com.weeth.domain.club.application.usecase.query.GetClubMemberQueryService
import com.weeth.domain.club.application.usecase.query.GetClubPositionOptionQueryService
import com.weeth.domain.club.application.usecase.query.GetClubQueryService
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class ClubAdminControllerTest :
    DescribeSpec({
        val manageClubUseCase = mockk<ManageClubUseCase>()
        val adminClubMemberUseCase = mockk<AdminClubMemberUseCase>()
        val manageClubPositionOptionUseCase = mockk<ManageClubPositionOptionUseCase>()
        val getClubQueryService = mockk<GetClubQueryService>()
        val getClubMemberQueryService = mockk<GetClubMemberQueryService>()
        val getClubPositionOptionQueryService = mockk<GetClubPositionOptionQueryService>()

        val controller =
            ClubAdminController(
                manageClubUseCase = manageClubUseCase,
                adminClubMemberUseCase = adminClubMemberUseCase,
                manageClubPositionOptionUseCase = manageClubPositionOptionUseCase,
                getClubQueryService = getClubQueryService,
                getClubMemberQueryService = getClubMemberQueryService,
                getClubPositionOptionQueryService = getClubPositionOptionQueryService,
            )

        val clubId = 1L
        val userId = 99L

        beforeTest {
            clearMocks(
                manageClubUseCase,
                adminClubMemberUseCase,
                manageClubPositionOptionUseCase,
                getClubQueryService,
                getClubMemberQueryService,
                getClubPositionOptionQueryService,
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

                every {
                    getClubMemberQueryService.searchClubMembers(
                        clubId = clubId,
                        userId = userId,
                        keyword = "홍길동",
                        cardinalNumber = null,
                        memberRole = null,
                    )
                } returns listOf(member)

                val response =
                    controller.searchClubMembers(
                        userId = userId,
                        clubId = clubId,
                        keyword = "홍길동",
                        cardinalNumber = null,
                        memberRole = null,
                    )

                response.code shouldBe ClubResponseCode.MEMBER_FIND_ALL_SUCCESS.code
                response.data?.size shouldBe 1
                response.data?.first()?.name shouldBe "홍길동"
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

                every {
                    getClubMemberQueryService.searchClubMembers(
                        clubId = clubId,
                        userId = userId,
                        keyword = "김",
                        cardinalNumber = 5,
                        memberRole = null,
                    )
                } returns listOf(member)

                val response =
                    controller.searchClubMembers(
                        userId = userId,
                        clubId = clubId,
                        keyword = "김",
                        cardinalNumber = 5,
                        memberRole = null,
                    )

                response.code shouldBe ClubResponseCode.MEMBER_FIND_ALL_SUCCESS.code
                response.data?.first()?.cardinals shouldBe listOf(5)
            }

            it("검색 결과가 없을 수 있다") {
                every {
                    getClubMemberQueryService.searchClubMembers(
                        clubId = clubId,
                        userId = userId,
                        keyword = "존재하지않음",
                        cardinalNumber = null,
                        memberRole = null,
                    )
                } returns emptyList()

                val response =
                    controller.searchClubMembers(
                        userId = userId,
                        clubId = clubId,
                        keyword = "존재하지않음",
                        cardinalNumber = null,
                        memberRole = null,
                    )

                response.code shouldBe ClubResponseCode.MEMBER_FIND_ALL_SUCCESS.code
                response.data?.size shouldBe 0
            }
        }

        describe("savePositionOptions") {
            it("저장 성공 코드를 반환한다") {
                val request =
                    SaveClubPositionOptionsRequest(
                        options = listOf(ClubPositionOptionRequest(name = "백엔드", colorHex = "#4CAF50")),
                    )
                every { manageClubPositionOptionUseCase.save(clubId, userId, request) } just Runs

                val response = controller.savePositionOptions(userId, clubId, request)

                response.code shouldBe ClubResponseCode.POSITION_OPTIONS_SAVED_SUCCESS.code
                verify(exactly = 1) { manageClubPositionOptionUseCase.save(clubId, userId, request) }
            }
        }

        describe("getPositionOptions") {
            it("조회 성공 코드와 옵션 목록을 반환한다") {
                val options =
                    listOf(
                        ClubPositionOptionResponse(id = 1L, name = "백엔드", colorHex = "#4CAF50", displayOrder = 0),
                        ClubPositionOptionResponse(id = 2L, name = "프론트엔드", colorHex = "#FF5722", displayOrder = 1),
                    )
                every { getClubPositionOptionQueryService.findAll(clubId, userId) } returns options

                val response = controller.getPositionOptions(userId, clubId)

                response.code shouldBe ClubResponseCode.POSITION_OPTIONS_FIND_SUCCESS.code
                response.data?.size shouldBe 2
            }
        }

        describe("updateMemberPosition") {
            it("포지션 지정 성공 코드를 반환한다") {
                val clubMemberId = 20L
                val request = ClubMemberPositionUpdateRequest(positionOptionId = 100L)
                every {
                    adminClubMemberUseCase.updateMemberPosition(clubId, userId, clubMemberId, request)
                } just Runs

                val response = controller.updateMemberPosition(userId, clubId, clubMemberId, request)

                response.code shouldBe ClubResponseCode.MEMBER_POSITION_UPDATED_SUCCESS.code
                verify(exactly = 1) {
                    adminClubMemberUseCase.updateMemberPosition(clubId, userId, clubMemberId, request)
                }
            }

            it("positionOptionId가 null이면 해제 요청을 그대로 전달한다") {
                val clubMemberId = 20L
                val request = ClubMemberPositionUpdateRequest(positionOptionId = null)
                every {
                    adminClubMemberUseCase.updateMemberPosition(clubId, userId, clubMemberId, request)
                } just Runs

                val response = controller.updateMemberPosition(userId, clubId, clubMemberId, request)

                response.code shouldBe ClubResponseCode.MEMBER_POSITION_UPDATED_SUCCESS.code
                verify(exactly = 1) {
                    adminClubMemberUseCase.updateMemberPosition(clubId, userId, clubMemberId, request)
                }
            }
        }
    })
