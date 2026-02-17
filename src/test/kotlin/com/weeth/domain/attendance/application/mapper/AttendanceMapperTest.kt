package com.weeth.domain.attendance.application.mapper

import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createActiveUser
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createActiveUserWithAttendances
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createAdminUserWithAttendances
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createAttendance
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createOneDayMeeting
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.enrichUserProfile
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.setAttendanceId
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.setUserAttendanceStats
import com.weeth.domain.user.domain.entity.enums.Position
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class AttendanceMapperTest :
    DescribeSpec({

        val mapper = AttendanceMapper()

        describe("toMainResponse") {
            it("사용자 + 당일 출석 객체를 MainResponse로 매핑한다") {
                val today = LocalDate.now()
                val meeting = createOneDayMeeting(today, 1, 1111, "Today")
                val user = createActiveUserWithAttendances("이지훈", listOf(meeting))
                val attendance = user.attendances[0]

                val main = mapper.toMainResponse(user, attendance)

                main.shouldNotBeNull()
                main.title shouldBe "Today"
                main.status shouldBe attendance.status
                main.start shouldBe meeting.start
                main.end shouldBe meeting.end
                main.location shouldBe meeting.location
            }

            it("attendance가 null이면 필드는 null로 매핑") {
                val user = createActiveUser("이지훈")

                val main = mapper.toMainResponse(user, null)

                main.shouldNotBeNull()
                main.title.shouldBeNull()
                main.start.shouldBeNull()
                main.end.shouldBeNull()
                main.location.shouldBeNull()
            }

            it("일반 유저는 출석 코드가 null로 매핑된다") {
                val today = LocalDate.now()
                val meeting = createOneDayMeeting(today, 1, 1234, "Today")
                val user = createActiveUserWithAttendances("일반유저", listOf(meeting))
                val attendance = user.attendances[0]

                val main = mapper.toMainResponse(user, attendance)

                main.shouldNotBeNull()
                main.code.shouldBeNull()
                main.title shouldBe "Today"
                main.status shouldBe attendance.status
            }
        }

        describe("toResponse") {
            it("단일 출석을 AttendanceResponse로 매핑한다") {
                val meeting = createOneDayMeeting(LocalDate.now().minusDays(1), 1, 2222, "D-1")
                val user = createActiveUser("사용자A")
                val attendance = createAttendance(meeting, user)

                val response = mapper.toResponse(attendance)

                response.shouldNotBeNull()
                response.title shouldBe "D-1"
                response.start shouldBe meeting.start
                response.end shouldBe meeting.end
                response.location shouldBe meeting.location
            }
        }

        describe("toDetailResponse") {
            it("사용자 + Response 리스트를 DetailResponse로 매핑(total = attend + absence)") {
                val base = LocalDate.now()
                val m1 = createOneDayMeeting(base.minusDays(2), 1, 1000, "D-2")
                val m2 = createOneDayMeeting(base.minusDays(1), 1, 1001, "D-1")
                val user = createActiveUser("이지훈")
                setUserAttendanceStats(user, 3, 2)

                val a1 = createAttendance(m1, user)
                val a2 = createAttendance(m2, user)

                val r1 = mapper.toResponse(a1)
                val r2 = mapper.toResponse(a2)

                val detail = mapper.toDetailResponse(user, listOf(r1, r2))

                detail.shouldNotBeNull()
                detail.attendances shouldBe listOf(r1, r2)
                detail.total shouldBe 5
            }
        }

        describe("toInfoResponse") {
            it("Attendance를 InfoResponse로 매핑") {
                val meeting = createOneDayMeeting(LocalDate.now(), 1, 3333, "Info")
                val user = createActiveUser("유저B")
                enrichUserProfile(user, Position.BE, "컴퓨터공학과", "20201234")

                val attendance = createAttendance(meeting, user)
                setAttendanceId(attendance, 10L)

                val info = mapper.toInfoResponse(attendance)

                info.shouldNotBeNull()
                info.id shouldBe 10L
                info.status shouldBe attendance.status
                info.name shouldBe "유저B"
            }
        }

        describe("toAdminResponse") {
            it("ADMIN 유저는 출석 코드가 포함된다") {
                val today = LocalDate.now()
                val expectedCode = 1234
                val meeting = createOneDayMeeting(today, 1, expectedCode, "Today")
                val adminUser = createAdminUserWithAttendances("관리자", listOf(meeting))
                val attendance = adminUser.attendances[0]

                val main = mapper.toAdminResponse(adminUser, attendance)

                main.shouldNotBeNull()
                main.code shouldBe expectedCode
                main.title shouldBe "Today"
                main.start shouldBe meeting.start
                main.end shouldBe meeting.end
                main.location shouldBe meeting.location
            }
        }
    })
