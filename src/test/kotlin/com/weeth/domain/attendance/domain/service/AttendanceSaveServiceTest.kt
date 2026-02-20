package com.weeth.domain.attendance.domain.service

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createActiveUser
import com.weeth.domain.schedule.fixture.ScheduleTestFixture.createMeeting
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class AttendanceSaveServiceTest :
    DescribeSpec({

        val attendanceRepository = mockk<AttendanceRepository>()
        val attendanceSaveService = AttendanceSaveService(attendanceRepository)

        describe("init") {
            it("각 정기모임에 대한 Attendance를 저장한다") {
                val user = mockk<com.weeth.domain.user.domain.entity.User>()
                val meetingFirst = createMeeting()
                val meetingSecond = createMeeting()

                every { attendanceRepository.save(any<Attendance>()) } answers { firstArg() }

                attendanceSaveService.init(user, listOf(meetingFirst, meetingSecond))

                verify(exactly = 2) { attendanceRepository.save(any<Attendance>()) }
            }
        }

        describe("saveAll") {
            it("사용자 수만큼 Attendance 생성 후 saveAll 호출") {
                val meeting = createMeeting()
                val userFirst = createActiveUser("이지훈")
                val userSecond = createActiveUser("이강혁")

                val listSlot = slot<List<Attendance>>()
                every { attendanceRepository.saveAll(capture(listSlot)) } answers { firstArg() }

                attendanceSaveService.saveAll(listOf(userFirst, userSecond), meeting)

                val savedAttendances = listSlot.captured
                savedAttendances shouldHaveSize 2
                savedAttendances.forEach { it.meeting shouldBe meeting }
                savedAttendances.map { it.user } shouldBe listOf(userFirst, userSecond)
            }
        }
    })
