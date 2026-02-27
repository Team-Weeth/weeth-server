package com.weeth.domain.attendance.application.mapper

import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createActiveUser
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createAdminUser
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createAttendance
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.enrichUserProfile
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.setAttendanceId
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.setUserAttendanceStats
import com.weeth.domain.session.fixture.SessionTestFixture.createOneDaySession
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class AttendanceMapperTest :
    DescribeSpec({
        val mapper = AttendanceMapper()

        describe("toSummaryResponse") {
            it("사용자 + 당일 출석 객체를 MainResponse로 매핑한다") {
                val today = LocalDate.now()
                val session = createOneDaySession(today, 1, 1111, "Today")
                val user = createActiveUser("이지훈")
                val attendance = createAttendance(session, user)

                val main = mapper.toSummaryResponse(user, attendance)

                main.shouldNotBeNull()
                main.title shouldBe session.title
                main.status shouldBe attendance.status
                main.start shouldBe session.start
                main.end shouldBe session.end
                main.location shouldBe session.location
            }

            it("attendance가 null이면 필드는 null로 매핑") {
                val user = createActiveUser("이지훈")

                val main = mapper.toSummaryResponse(user, null)

                main.shouldNotBeNull()
                main.title.shouldBeNull()
                main.start.shouldBeNull()
                main.end.shouldBeNull()
                main.location.shouldBeNull()
            }

            it("일반 유저는 출석 코드가 null로 매핑된다") {
                val today = LocalDate.now()
                val session = createOneDaySession(today, 1, 1234, "Today")
                val user = createActiveUser("일반유저")
                val attendance = createAttendance(session, user)

                val main = mapper.toSummaryResponse(user, attendance)

                main.shouldNotBeNull()
                main.code.shouldBeNull()
                main.title shouldBe session.title
                main.status shouldBe attendance.status
            }

            it("ADMIN 유저는 출석 코드가 포함된다") {
                val today = LocalDate.now()
                val expectedCode = 1234
                val session = createOneDaySession(today, 1, expectedCode, "Today")
                val adminUser = createAdminUser("관리자")
                val attendance = createAttendance(session, adminUser)

                val main = mapper.toSummaryResponse(adminUser, attendance, isAdmin = true)

                main.shouldNotBeNull()
                main.code shouldBe expectedCode
                main.title shouldBe session.title
                main.start shouldBe session.start
                main.end shouldBe session.end
                main.location shouldBe session.location
            }
        }

        describe("toResponse") {
            it("단일 출석을 AttendanceResponse로 매핑한다") {
                val session = createOneDaySession(LocalDate.now().minusDays(1), 1, 2222, "D-1")
                val user = createActiveUser("사용자A")
                val attendance = createAttendance(session, user)

                val response = mapper.toResponse(attendance)

                response.shouldNotBeNull()
                response.title shouldBe session.title
                response.start shouldBe session.start
                response.end shouldBe session.end
                response.location shouldBe session.location
            }
        }

        describe("toDetailResponse") {
            it("사용자 + Response 리스트를 DetailResponse로 매핑(total = attend + absence)") {
                val base = LocalDate.now()
                val m1 = createOneDaySession(base.minusDays(2), 1, 1000, "D-2")
                val m2 = createOneDaySession(base.minusDays(1), 1, 1001, "D-1")
                val user = createActiveUser("이지훈")
                setUserAttendanceStats(user, 3, 2)

                val a1 = createAttendance(m1, user)
                val a2 = createAttendance(m2, user)

                val r1 = mapper.toResponse(a1)
                val r2 = mapper.toResponse(a2)

                val detail = mapper.toDetailResponse(user, listOf(r1, r2))

                detail.shouldNotBeNull()
                detail.attendances shouldBe listOf(r1, r2)
                detail.total shouldBe user.attendanceCount + user.absenceCount
            }
        }

        describe("toInfoResponse") {
            it("Attendance를 InfoResponse로 매핑") {
                val session = createOneDaySession(LocalDate.now(), 1, 3333, "Info")
                val user = createActiveUser("유저B")
                enrichUserProfile(user, "컴퓨터공학과", "20201234")

                val attendance = createAttendance(session, user)
                setAttendanceId(attendance, 10L)

                val info = mapper.toInfoResponse(attendance)

                info.shouldNotBeNull()
                info.id shouldBe attendance.id
                info.status shouldBe attendance.status
                info.name shouldBe user.name
                info.department shouldBe user.department
            }
        }
    })
