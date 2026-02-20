package com.weeth.domain.attendance.domain.service

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createActiveUser
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createOneDaySession
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDate

class AttendanceSaveServiceTest :
    DescribeSpec({

        val attendanceRepository = mockk<AttendanceRepository>()
        val attendanceSaveService = AttendanceSaveService(attendanceRepository)

        describe("init") {
            it("각 정기모임에 대한 Attendance를 생성하여 저장한다") {
                val user = createActiveUser("이지훈")
                val sessionFirst = createOneDaySession(LocalDate.now(), 1, 1234, "1차")
                val sessionSecond = createOneDaySession(LocalDate.now().plusDays(7), 1, 5678, "2차")

                every { attendanceRepository.save(any<Attendance>()) } answers { firstArg() }

                attendanceSaveService.init(user, listOf(sessionFirst, sessionSecond))

                verify(exactly = 2) { attendanceRepository.save(any<Attendance>()) }
            }
        }

        describe("saveAll") {
            it("사용자 수만큼 Attendance 생성 후 saveAll 호출") {
                val session = createOneDaySession(LocalDate.now(), 1, 1234, "1차")
                val userFirst = createActiveUser("이지훈")
                val userSecond = createActiveUser("이강혁")

                val listSlot = slot<List<Attendance>>()
                every { attendanceRepository.saveAll(capture(listSlot)) } answers { firstArg() }

                attendanceSaveService.saveAll(listOf(userFirst, userSecond), session)

                val savedAttendances = listSlot.captured
                savedAttendances shouldHaveSize 2
                savedAttendances.forEach { it.session shouldBe session }
                savedAttendances.map { it.user } shouldBe listOf(userFirst, userSecond)
            }
        }
    })
