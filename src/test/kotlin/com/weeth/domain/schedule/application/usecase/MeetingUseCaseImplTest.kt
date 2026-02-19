package com.weeth.domain.schedule.application.usecase

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.entity.enums.Status
import com.weeth.domain.attendance.domain.service.AttendanceDeleteService
import com.weeth.domain.attendance.domain.service.AttendanceGetService
import com.weeth.domain.attendance.domain.service.AttendanceSaveService
import com.weeth.domain.attendance.domain.service.AttendanceUpdateService
import com.weeth.domain.schedule.application.dto.MeetingDTO
import com.weeth.domain.schedule.application.dto.ScheduleDTO
import com.weeth.domain.schedule.application.mapper.MeetingMapper
import com.weeth.domain.schedule.domain.entity.Meeting
import com.weeth.domain.schedule.domain.entity.enums.Type
import com.weeth.domain.schedule.domain.service.MeetingDeleteService
import com.weeth.domain.schedule.domain.service.MeetingGetService
import com.weeth.domain.schedule.domain.service.MeetingSaveService
import com.weeth.domain.schedule.domain.service.MeetingUpdateService
import com.weeth.domain.schedule.fixture.ScheduleTestFixture
import com.weeth.domain.user.domain.entity.Cardinal
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.domain.user.domain.service.CardinalGetService
import com.weeth.domain.user.domain.service.UserGetService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

class MeetingUseCaseImplTest :
    DescribeSpec({
        val meetingGetService = mockk<MeetingGetService>()
        val meetingMapper = mockk<MeetingMapper>()
        val meetingSaveService = mockk<MeetingSaveService>(relaxUnitFun = true)
        val userGetService = mockk<UserGetService>()
        val meetingUpdateService = mockk<MeetingUpdateService>(relaxUnitFun = true)
        val meetingDeleteService = mockk<MeetingDeleteService>(relaxUnitFun = true)
        val attendanceGetService = mockk<AttendanceGetService>()
        val attendanceSaveService = mockk<AttendanceSaveService>(relaxUnitFun = true)
        val attendanceDeleteService = mockk<AttendanceDeleteService>(relaxUnitFun = true)
        val attendanceUpdateService = mockk<AttendanceUpdateService>(relaxUnitFun = true)
        val cardinalGetService = mockk<CardinalGetService>()
        val em = mockk<EntityManager>(relaxUnitFun = true)

        val useCase =
            MeetingUseCaseImpl(
                meetingGetService,
                meetingMapper,
                meetingSaveService,
                userGetService,
                meetingUpdateService,
                meetingDeleteService,
                attendanceGetService,
                attendanceSaveService,
                attendanceDeleteService,
                attendanceUpdateService,
                cardinalGetService,
            )

        beforeSpec {
            ReflectionTestUtils.setField(useCase, "em", em)
        }

        beforeTest {
            clearMocks(
                meetingGetService,
                meetingMapper,
                meetingSaveService,
                userGetService,
                meetingUpdateService,
                meetingDeleteService,
                attendanceGetService,
                attendanceSaveService,
                attendanceDeleteService,
                attendanceUpdateService,
                cardinalGetService,
                em,
            )
        }

        describe("find(userId, meetingId)") {
            val meetingId = 1L
            val userId = 10L
            val meeting = ScheduleTestFixture.createMeeting(id = meetingId)

            context("ADMIN 유저일 때") {
                it("toAdminResponse로 매핑한다") {
                    val adminUser = mockk<User>()
                    every { adminUser.role } returns Role.ADMIN
                    every { userGetService.find(userId) } returns adminUser
                    every { meetingGetService.find(meetingId) } returns meeting
                    val adminResponse = mockk<MeetingDTO.Response>()
                    every { meetingMapper.toAdminResponse(meeting) } returns adminResponse

                    val result = useCase.find(userId, meetingId)

                    result shouldBe adminResponse
                    verify { meetingMapper.toAdminResponse(meeting) }
                }
            }

            context("일반 유저일 때") {
                it("to(meeting)으로 매핑한다 (코드 미노출)") {
                    val normalUser = mockk<User>()
                    every { normalUser.role } returns Role.USER
                    every { userGetService.find(userId) } returns normalUser
                    every { meetingGetService.find(meetingId) } returns meeting
                    val normalResponse = mockk<MeetingDTO.Response>()
                    every { meetingMapper.to(meeting) } returns normalResponse

                    val result = useCase.find(userId, meetingId)

                    result shouldBe normalResponse
                    verify { meetingMapper.to(meeting) }
                }
            }
        }

        describe("find(cardinal)") {
            it("이번 주 정기모임이 있으면 thisWeek에 포함된다") {
                val now = LocalDateTime.now()
                val thisWeekMeeting =
                    ScheduleTestFixture.createMeeting(
                        id = 1L,
                        title = "This Week",
                        start = now,
                        end = now.plusHours(2),
                    )
                val lastWeekMeeting =
                    ScheduleTestFixture.createMeeting(
                        id = 2L,
                        title = "Last Week",
                        start = now.minusDays(14),
                        end = now.minusDays(14).plusHours(2),
                    )
                val thisWeekInfo = MeetingDTO.Info(1L, 1, "This Week", now)
                val lastWeekInfo = MeetingDTO.Info(2L, 1, "Last Week", now.minusDays(14))

                every { meetingGetService.findMeetingByCardinal(1) } returns listOf(thisWeekMeeting, lastWeekMeeting)
                every { meetingMapper.toInfo(thisWeekMeeting) } returns thisWeekInfo
                every { meetingMapper.toInfo(lastWeekMeeting) } returns lastWeekInfo

                val result = useCase.find(1)

                result.thisWeek shouldNotBe null
                result.thisWeek.title() shouldBe "This Week"
            }
        }

        describe("save") {
            it("정기모임 저장 후 해당 기수 전체 유저에게 출석을 생성한다") {
                val userId = 10L
                val user = mockk<User>()
                val cardinal = mockk<Cardinal>()
                val userList = listOf(mockk<User>(), mockk<User>(), mockk<User>())
                val meeting = ScheduleTestFixture.createMeeting()
                val dto =
                    ScheduleDTO.Save(
                        "Title",
                        "Content",
                        "Location",
                        null,
                        Type.MEETING,
                        1,
                        LocalDateTime.of(2026, 3, 1, 10, 0),
                        LocalDateTime.of(2026, 3, 1, 12, 0),
                    )

                every { userGetService.find(userId) } returns user
                every { cardinalGetService.findByUserSide(1) } returns cardinal
                every { userGetService.findAllByCardinal(cardinal) } returns userList
                every { meetingMapper.from(dto, user) } returns meeting

                useCase.save(dto, userId)

                verify(exactly = 1) { meetingSaveService.save(meeting) }
                verify(exactly = 1) { attendanceSaveService.saveAll(userList, meeting) }
            }
        }

        describe("delete") {
            it("출석 통계 롤백 후 출석 삭제 → 정기모임 삭제 순서로 처리한다") {
                val meetingId = 1L
                val meeting = ScheduleTestFixture.createMeeting(id = meetingId)
                val attendance1 = mockk<Attendance>()
                val attendance2 = mockk<Attendance>()
                val attendances = listOf(attendance1, attendance2)

                every { meetingGetService.find(meetingId) } returns meeting
                every { attendanceGetService.findAllByMeeting(meeting) } returns attendances

                useCase.delete(meetingId)

                verify(ordering = io.mockk.Ordering.ORDERED) {
                    attendanceUpdateService.updateUserAttendanceByStatus(attendances)
                    em.flush()
                    em.clear()
                    attendanceDeleteService.deleteAll(meeting)
                    meetingDeleteService.delete(meeting)
                }
            }
        }
    })
