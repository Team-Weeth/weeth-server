package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.session.domain.repository.SessionReader
import com.weeth.domain.user.application.dto.request.UserApplyObRequest
import com.weeth.domain.user.application.dto.request.UserIdsRequest
import com.weeth.domain.user.application.dto.request.UserRoleUpdateRequest
import com.weeth.domain.user.domain.entity.UserCardinal
import com.weeth.domain.user.domain.enums.Role
import com.weeth.domain.user.domain.enums.Status
import com.weeth.domain.user.domain.repository.UserCardinalRepository
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.domain.service.UserCardinalPolicy
import com.weeth.domain.user.fixture.SessionTestFixture
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
        val userCardinalPolicy = mockk<UserCardinalPolicy>()
        val cardinalReader = mockk<CardinalReader>()
        val sessionReader = mockk<SessionReader>()
        val attendanceRepository = mockk<AttendanceRepository>()
        val userCardinalRepository = mockk<UserCardinalRepository>()

        val useCase =
            AdminUserUseCase(
                userReader = userReader,
                userCardinalPolicy = userCardinalPolicy,
                cardinalReader = cardinalReader,
                sessionReader = sessionReader,
                attendanceRepository = attendanceRepository,
                userCardinalRepository = userCardinalRepository,
            )

        beforeTest {
            clearMocks(
                userReader,
                userCardinalPolicy,
                cardinalReader,
                sessionReader,
                attendanceRepository,
                userCardinalRepository,
            )
        }

        describe("accept") {
            it("비활성 유저 승인 시 출석 초기화를 수행한다") {
                val user = UserTestFixture.createWaitingUser1(1L)
                val currentCardinal =
                    CardinalTestFixture.createCardinal(
                        id = 1L,
                        cardinalNumber = 8,
                        year = 2025,
                        semester = 1,
                    )
                val sessions = listOf(SessionTestFixture.createSession(cardinalNumber = 8))

                every { userReader.findAllByIds(listOf(1L)) } returns listOf(user)
                every { userCardinalPolicy.getCurrentCardinal(user) } returns currentCardinal
                every { sessionReader.findAllByCardinal(8) } returns sessions
                every { attendanceRepository.saveAll(any<List<Attendance>>()) } answers { firstArg() }

                useCase.accept(UserIdsRequest(listOf(1L)))

                verify(exactly = 1) { attendanceRepository.saveAll(any<List<Attendance>>()) }
                user.status shouldBe Status.ACTIVE
            }

            it("이미 활성 상태인 유저는 승인 처리를 건너뛴다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val currentCardinal =
                    CardinalTestFixture.createCardinal(
                        id = 1L,
                        cardinalNumber = 8,
                        year = 2025,
                        semester = 1,
                    )

                every { userReader.findAllByIds(listOf(1L)) } returns listOf(user)
                every { userCardinalPolicy.getCurrentCardinal(user) } returns currentCardinal

                useCase.accept(UserIdsRequest(listOf(1L)))

                user.status shouldBe Status.ACTIVE
                verify(exactly = 0) { sessionReader.findAllByCardinal(any()) }
                verify(exactly = 0) { attendanceRepository.saveAll(any<List<Attendance>>()) }
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
            it("중복 요청을 제거하고 새 기수에 등록한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val nextCardinal =
                    CardinalTestFixture.createCardinal(
                        id = 2L,
                        cardinalNumber = 4,
                        year = 2024,
                        semester = 2,
                    )
                val sessions = listOf(SessionTestFixture.createSession(cardinalNumber = 4))

                val requests =
                    listOf(
                        UserApplyObRequest(userId = 1L, cardinal = 4),
                        UserApplyObRequest(userId = 1L, cardinal = 4),
                    )

                every { userReader.findAllByIds(listOf(1L)) } returns listOf(user)
                every { cardinalReader.getByCardinalNumber(4) } returns nextCardinal
                every { userCardinalPolicy.notContains(user, nextCardinal) } returns true
                every { userCardinalPolicy.isCurrent(user, nextCardinal) } returns true
                every { sessionReader.findAllByCardinal(4) } returns sessions
                every { attendanceRepository.saveAll(any<List<Attendance>>()) } answers { firstArg() }
                every { userCardinalRepository.save(any<UserCardinal>()) } answers { firstArg() }

                useCase.applyOb(requests)

                // 중복 제거되어 1번만 실행
                verify(exactly = 1) { userCardinalRepository.save(any<UserCardinal>()) }
                verify(exactly = 1) { attendanceRepository.saveAll(any<List<Attendance>>()) }
            }

            it("새 기수이지만 현재 기수보다 이전이면 출석 초기화 없이 등록만 한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val nextCardinal =
                    CardinalTestFixture.createCardinal(
                        id = 2L,
                        cardinalNumber = 3,
                        year = 2023,
                        semester = 2,
                    )

                every { userReader.findAllByIds(listOf(1L)) } returns listOf(user)
                every { cardinalReader.getByCardinalNumber(3) } returns nextCardinal
                every { userCardinalPolicy.notContains(user, nextCardinal) } returns true
                every { userCardinalPolicy.isCurrent(user, nextCardinal) } returns false
                every { userCardinalRepository.save(any<UserCardinal>()) } answers { firstArg() }

                useCase.applyOb(listOf(UserApplyObRequest(userId = 1L, cardinal = 3)))

                verify(exactly = 1) { userCardinalRepository.save(any<UserCardinal>()) }
                verify(exactly = 0) { sessionReader.findAllByCardinal(any()) }
                verify(exactly = 0) { attendanceRepository.saveAll(any<List<Attendance>>()) }
            }

            it("이미 등록된 기수이면 건너뛴다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val nextCardinal =
                    CardinalTestFixture.createCardinal(
                        id = 2L,
                        cardinalNumber = 4,
                        year = 2024,
                        semester = 2,
                    )

                every { userReader.findAllByIds(listOf(1L)) } returns listOf(user)
                every { cardinalReader.getByCardinalNumber(4) } returns nextCardinal
                every { userCardinalPolicy.notContains(user, nextCardinal) } returns false

                useCase.applyOb(listOf(UserApplyObRequest(userId = 1L, cardinal = 4)))

                verify(exactly = 0) { userCardinalRepository.save(any<UserCardinal>()) }
            }

            it("요청이 비어 있으면 아무 작업도 수행하지 않는다") {
                useCase.applyOb(emptyList())

                verify(exactly = 0) { userReader.findAllByIds(any()) }
                verify(exactly = 0) { userCardinalRepository.save(any<UserCardinal>()) }
            }
        }
    })
