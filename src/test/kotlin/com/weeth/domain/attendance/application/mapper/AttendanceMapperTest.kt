package com.weeth.domain.attendance.application.mapper

import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createActiveUser
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createAttendance
import com.weeth.domain.club.fixture.ClubMemberTestFixture
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
                val member =
                    ClubMemberTestFixture.createActiveMember(
                        club = session.club,
                        user = createActiveUser("이지훈"),
                    )
                val attendance = createAttendance(session, member)

                val main = mapper.toSummaryResponse(member, attendance)

                main.shouldNotBeNull()
                main.title shouldBe session.title
                main.status shouldBe attendance.status
                main.start shouldBe session.start
                main.end shouldBe session.end
                main.location shouldBe session.location
            }

            it("attendance가 null이면 필드는 null로 매핑") {
                val member = ClubMemberTestFixture.createActiveMember(user = createActiveUser("이지훈"))

                val main = mapper.toSummaryResponse(member, null)

                main.shouldNotBeNull()
                main.title.shouldBeNull()
                main.start.shouldBeNull()
                main.end.shouldBeNull()
                main.location.shouldBeNull()
            }
        }

        describe("toDetailResponse") {
            it("조회 기록의 상태만으로 통계를 계산하고 누적값은 사용하지 않는다") {
                val base = LocalDate.now()
                val m1 = createOneDaySession(base.minusDays(2), 1, 1000, "D-2")
                val m2 = createOneDaySession(base.minusDays(1), 1, 1001, "D-1", club = m1.club)
                val member = ClubMemberTestFixture.createActiveMember(club = m1.club, user = createActiveUser("이지훈"))
                repeat(3) { member.attend() }
                repeat(2) { member.absent() }

                val a1 = createAttendance(m1, member).also { it.attend() }
                val a2 = createAttendance(m2, member).also { it.absent() }

                val r1 = mapper.toResponse(a1)
                val r2 = mapper.toResponse(a2)

                val detail = mapper.toDetailResponse(1, listOf(a1, a2))

                detail.shouldNotBeNull()
                detail.attendances shouldBe listOf(r1, r2)
                detail.total shouldBe 2
                detail.attendanceRate shouldBe 50
                detail.cardinalNumber shouldBe 1
            }
        }
    })
