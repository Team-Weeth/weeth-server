package com.weeth.domain.user.application.usecase

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.entity.Session
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.attendance.domain.repository.SessionRepository
import com.weeth.domain.user.application.dto.request.UserRequestDto
import com.weeth.domain.user.application.dto.response.UserResponseDto
import com.weeth.domain.user.application.exception.InvalidUserOrderException
import com.weeth.domain.user.application.mapper.UserMapper
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.UserCardinal
import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.domain.user.domain.entity.enums.Status
import com.weeth.domain.user.domain.entity.enums.UsersOrderBy
import com.weeth.domain.user.domain.service.CardinalGetService
import com.weeth.domain.user.domain.service.UserCardinalGetService
import com.weeth.domain.user.domain.service.UserCardinalSaveService
import com.weeth.domain.user.domain.service.UserDeleteService
import com.weeth.domain.user.domain.service.UserGetService
import com.weeth.domain.user.domain.service.UserUpdateService
import com.weeth.domain.user.fixture.CardinalTestFixture
import com.weeth.domain.user.fixture.UserTestFixture
import com.weeth.global.auth.jwt.domain.port.RefreshTokenStorePort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime

class UserManageUseCaseTest :
    DescribeSpec({

        val userGetService = mockk<UserGetService>()
        val userUpdateService = mockk<UserUpdateService>(relaxUnitFun = true)
        val userDeleteService = mockk<UserDeleteService>(relaxUnitFun = true)
        val attendanceSaveService = mockk<AttendanceSaveService>(relaxUnitFun = true)
        val meetingGetService = mockk<MeetingGetService>()
        val refreshTokenStorePort = mockk<RefreshTokenStorePort>(relaxUnitFun = true)
        val cardinalGetService = mockk<CardinalGetService>()
        val userCardinalSaveService = mockk<UserCardinalSaveService>(relaxUnitFun = true)
        val userCardinalGetService = mockk<UserCardinalGetService>()
        val userMapper = mockk<UserMapper>()
        val passwordEncoder = mockk<PasswordEncoder>()

        val useCase =
            UserManageUseCaseImpl(
                userGetService,
                userUpdateService,
                userDeleteService,
                attendanceSaveService,
                meetingGetService,
                refreshTokenStorePort,
                cardinalGetService,
                userCardinalSaveService,
                userCardinalGetService,
                userMapper,
                passwordEncoder,
            )

        describe("findAllByAdmin") {
            context("orderBy가 null이면") {
                it("예외가 발생한다") {
                    shouldThrow<InvalidUserOrderException> {
                        useCase.findAllByAdmin(null)
                    }
                }
            }

            context("orderBy에 맞게 정렬하여 조회할 때") {
                it("정렬된 결과를 반환한다") {
                    val user1 = UserTestFixture.createActiveUser1()
                    val user2 = UserTestFixture.createWaitingUser2()
                    val cd1 = CardinalTestFixture.createCardinal(id = 1L, cardinalNumber = 6, year = 2020, semester = 2)
                    val cd2 = CardinalTestFixture.createCardinal(id = 2L, cardinalNumber = 7, year = 2021, semester = 1)
                    val uc1 = UserCardinal(user1, cd1)
                    val uc2 = UserCardinal(user2, cd2)

                    val adminResponse1 =
                        UserResponseDto.AdminResponse(
                            1,
                            "aaa",
                            "a@a.com",
                            "202034420",
                            "01011112222",
                            "산업공학과",
                            listOf(6),
                            null,
                            Status.ACTIVE,
                            null,
                            0,
                            0,
                            0,
                            0,
                            0,
                            LocalDateTime.now().minusDays(3),
                            LocalDateTime.now(),
                        )
                    val adminResponse2 =
                        UserResponseDto.AdminResponse(
                            2,
                            "bbb",
                            "b@b.com",
                            "202045678",
                            "01033334444",
                            "컴퓨터공학과",
                            listOf(7),
                            null,
                            Status.WAITING,
                            null,
                            0,
                            0,
                            0,
                            0,
                            0,
                            LocalDateTime.now().minusDays(2),
                            LocalDateTime.now(),
                        )

                    every { userCardinalGetService.getUserCardinals(user1) } returns listOf(uc1)
                    every { userCardinalGetService.getUserCardinals(user2) } returns listOf(uc2)
                    every { userCardinalGetService.findAll() } returns listOf(uc2, uc1)
                    every { userMapper.toAdminResponse(user1, listOf(uc1)) } returns adminResponse1
                    every { userMapper.toAdminResponse(user2, listOf(uc2)) } returns adminResponse2

                    val result = useCase.findAllByAdmin(UsersOrderBy.NAME_ASCENDING)

                    result shouldHaveSize 2
                    result[0].name() shouldBe "aaa"
                    result[1].name() shouldBe "bbb"
                }
            }
        }

        describe("accept") {
            it("비활성유저 승인시 출석초기화가 정상 호출된다") {
                val user1 = UserTestFixture.createWaitingUser1(1L)
                val userIds = UserRequestDto.UserId(listOf(1L))
                val cardinal = CardinalTestFixture.createCardinal(id = 1L, cardinalNumber = 8, year = 2020, semester = 2)
                val session = mockk<Session>()

                every { userGetService.findAll(userIds.userId()) } returns listOf(user1)
                every { userCardinalGetService.getCurrentCardinal(user1) } returns cardinal
                every { sessionRepository.findAllByCardinalOrderByStartAsc(8) } returns listOf(session)

                useCase.accept(userIds)

                verify { userUpdateService.accept(user1) }
                verify { attendanceRepository.saveAll(any<List<Attendance>>()) }
            }
        }

        describe("update") {
            it("유저권한변경시 DB와 Redis 모두 갱신된다") {
                val user1 = UserTestFixture.createActiveUser1(1L)
                val request = UserRequestDto.UserRoleUpdate(1L, Role.ADMIN)

                every { userGetService.find(1L) } returns user1

                useCase.update(listOf(request))

                verify { userUpdateService.update(user1, "ADMIN") }
                verify { refreshTokenStorePort.updateRole(1L, Role.ADMIN) }
            }
        }

        describe("leave") {
            it("회원탈퇴시 토큰무효화 및 유저상태변경된다") {
                val user1 = UserTestFixture.createActiveUser1(1L)
                every { userGetService.find(1L) } returns user1

                useCase.leave(1L)

                verify { refreshTokenStorePort.delete(1L) }
                verify { userDeleteService.leave(user1) }
            }
        }

        describe("ban") {
            it("회원ban시 토큰무효화 및 유저상태변경된다") {
                val user1 = UserTestFixture.createActiveUser1(1L)
                val ids = UserRequestDto.UserId(listOf(1L))
                every { userGetService.findAll(ids.userId()) } returns listOf(user1)

                useCase.ban(ids)

                verify { refreshTokenStorePort.delete(1L) }
                verify { userDeleteService.ban(user1) }
            }
        }

        describe("applyOB") {
            it("현재기수 OB신청시 출석초기화 및 기수업데이트된다") {
                val user =
                    User
                        .builder()
                        .id(1L)
                        .name("aaa")
                        .status(Status.ACTIVE)
                        .build()
                val nextCardinal = CardinalTestFixture.createCardinal(id = 1L, cardinalNumber = 4, year = 2020, semester = 2)
                val request = UserRequestDto.UserApplyOB(1L, 4)
                val session = mockk<Session>()

                every { userGetService.find(1L) } returns user
                every { cardinalGetService.findByAdminSide(4) } returns nextCardinal
                every { userCardinalGetService.notContains(user, nextCardinal) } returns true
                every { userCardinalGetService.isCurrent(user, nextCardinal) } returns true
                every { sessionRepository.findAllByCardinalOrderByStartAsc(4) } returns listOf(session)

                useCase.applyOB(listOf(request))

                verify { attendanceRepository.saveAll(any<List<Attendance>>()) }
                verify { userCardinalSaveService.save(any<UserCardinal>()) }
            }
        }

        describe("reset") {
            it("비밀번호초기화시 모든유저에 reset이 호출된다") {
                val user1 = UserTestFixture.createActiveUser1(1L)
                val user2 = UserTestFixture.createActiveUser2(2L)
                val ids = UserRequestDto.UserId(listOf(1L, 2L))

                every { userGetService.findAll(ids.userId()) } returns listOf(user1, user2)

                useCase.reset(ids)

                verify { userGetService.findAll(ids.userId()) }
                verify { userUpdateService.reset(user1, passwordEncoder) }
                verify { userUpdateService.reset(user2, passwordEncoder) }
            }
        }
    })
