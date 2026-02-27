package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.attendance.domain.service.AttendanceSaveService
import com.weeth.domain.schedule.domain.entity.Meeting
import com.weeth.domain.schedule.domain.service.MeetingGetService
import com.weeth.domain.user.application.dto.request.UserApplyObRequest
import com.weeth.domain.user.application.dto.request.UserIdsRequest
import com.weeth.domain.user.application.dto.request.UserRoleUpdateRequest
import com.weeth.domain.user.domain.entity.UserCardinal
import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.domain.user.domain.entity.enums.Status
import com.weeth.domain.user.domain.repository.CardinalRepository
import com.weeth.domain.user.domain.repository.UserCardinalRepository
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.domain.service.UserCardinalPolicy
import com.weeth.domain.user.fixture.CardinalTestFixture
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class AdminUserUseCaseTest :
    DescribeSpec({
        val userReader = mockk<UserReader>()
        val attendanceSaveService = mockk<AttendanceSaveService>(relaxUnitFun = true)
        val meetingGetService = mockk<MeetingGetService>()
        val cardinalRepository = mockk<CardinalRepository>()
        val userCardinalRepository = mockk<UserCardinalRepository>(relaxUnitFun = true)
        val userCardinalPolicy = mockk<UserCardinalPolicy>()

        val useCase =
            AdminUserUseCase(
                userReader,
                attendanceSaveService,
                meetingGetService,
                cardinalRepository,
                userCardinalRepository,
                userCardinalPolicy,
            )

        beforeTest {
            clearMocks(
                userReader,
                attendanceSaveService,
                meetingGetService,
                cardinalRepository,
                userCardinalRepository,
                userCardinalPolicy,
            )
        }

        describe("accept") {
            it("비활성 유저 승인 시 출석 초기화를 수행한다") {
                val user = UserTestFixture.createWaitingUser1(1L)
                val currentCardinal = CardinalTestFixture.createCardinal(id = 1L, cardinalNumber = 8, year = 2025, semester = 1)
                val meetings = listOf(mockk<Meeting>())

                every { userReader.findAllByIds(listOf(1L)) } returns listOf(user)
                every { userCardinalPolicy.getCurrentCardinal(user) } returns currentCardinal
                every { meetingGetService.find(8) } returns meetings

                useCase.accept(UserIdsRequest(listOf(1L)))

                verify(exactly = 1) { attendanceSaveService.init(user, meetings) }
                user.status shouldBe Status.ACTIVE
            }
        }

        describe("updateRole") {
            it("권한 변경 시 엔티티 권한을 갱신한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                every { userReader.getById(1L) } returns user

                useCase.updateRole(listOf(UserRoleUpdateRequest(1L, Role.ADMIN)))

                user.role shouldBe Role.ADMIN
            }
        }

        describe("ban") {
            it("회원 추방 시 상태를 BANNED로 변경한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                every { userReader.findAllByIds(listOf(1L)) } returns listOf(user)

                useCase.ban(UserIdsRequest(listOf(1L)))

                user.status shouldBe Status.BANNED
            }
        }

        describe("applyOb") {
            it("다음 기수로 OB 신청 시 출석을 초기화하고 user-cardinal을 저장한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val currentCardinal = CardinalTestFixture.createCardinal(id = 10L, cardinalNumber = 3, year = 2024, semester = 2)
                val nextCardinal = CardinalTestFixture.createCardinal(id = 11L, cardinalNumber = 4, year = 2025, semester = 1)
                val meetings = listOf(mockk<Meeting>())
                val request = listOf(UserApplyObRequest(1L, 4))

                every { userReader.findAllByIds(listOf(1L)) } returns listOf(user)
                every { userCardinalRepository.findAllByUsers(listOf(user)) } returns listOf(UserCardinal(user, currentCardinal))
                every { cardinalRepository.findAllByCardinalNumberIn(listOf(4)) } returns listOf(nextCardinal)
                every { meetingGetService.findByCardinals(listOf(4)) } returns mapOf(4 to meetings)
                every { userCardinalRepository.save(any()) } answers { firstArg() }

                useCase.applyOb(request)

                verify(exactly = 1) { attendanceSaveService.init(user, meetings) }
                verify(exactly = 1) { userCardinalRepository.save(match { it.user == user && it.cardinal == nextCardinal }) }
            }

            it("이미 해당 기수를 보유한 유저는 저장을 스킵한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val cardinal = CardinalTestFixture.createCardinal(id = 11L, cardinalNumber = 4, year = 2025, semester = 1)
                val request = listOf(UserApplyObRequest(1L, 4))

                every { userReader.findAllByIds(listOf(1L)) } returns listOf(user)
                every { userCardinalRepository.findAllByUsers(listOf(user)) } returns listOf(UserCardinal(user, cardinal))
                every { cardinalRepository.findAllByCardinalNumberIn(listOf(4)) } returns listOf(cardinal)

                useCase.applyOb(request)

                verify(exactly = 0) { meetingGetService.findByCardinals(any()) }
                verify(exactly = 0) { userCardinalRepository.save(any()) }
                verify(exactly = 0) { attendanceSaveService.init(any(), any()) }
            }

            it("요청 목록이 비어 있으면 아무 처리도 하지 않는다") {
                useCase.applyOb(emptyList())

                verify(exactly = 0) { userReader.findAllByIds(any()) }
                verify(exactly = 0) { userCardinalRepository.save(any()) }
            }

            it("존재하지 않는 기수라면 새로 생성한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val currentCardinal = CardinalTestFixture.createCardinal(id = 10L, cardinalNumber = 3, year = 2024, semester = 2)
                val createdCardinal = CardinalTestFixture.createCardinal(id = 12L, cardinalNumber = 5, year = 2025, semester = 2)
                val meetings = listOf(mockk<Meeting>())
                val request = listOf(UserApplyObRequest(1L, 5))

                every { userReader.findAllByIds(listOf(1L)) } returns listOf(user)
                every { userCardinalRepository.findAllByUsers(listOf(user)) } returns listOf(UserCardinal(user, currentCardinal))
                every { cardinalRepository.findAllByCardinalNumberIn(listOf(5)) } returns emptyList()
                every { cardinalRepository.save(any()) } returns createdCardinal
                every { meetingGetService.findByCardinals(listOf(5)) } returns mapOf(5 to meetings)
                every { userCardinalRepository.save(any()) } answers { firstArg() }

                useCase.applyOb(request)

                verify(exactly = 1) { cardinalRepository.save(any()) }
                verify(exactly = 1) { attendanceSaveService.init(user, meetings) }
                verify(exactly = 1) { userCardinalRepository.save(match { it.user == user && it.cardinal == createdCardinal }) }
            }
        }
    })
