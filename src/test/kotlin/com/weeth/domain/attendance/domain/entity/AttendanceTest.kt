package com.weeth.domain.attendance.domain.entity

import com.weeth.domain.attendance.domain.entity.enums.AttendanceStatus
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createActiveUser
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createAttendance
import com.weeth.domain.session.fixture.SessionTestFixture.createOneDaySession
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class AttendanceTest :
    DescribeSpec({

        val session = createOneDaySession(LocalDate.now(), 1, 1234, "테스트")

        describe("attend") {
            it("상태를 ATTEND로 변경한다") {
                val user = createActiveUser("테스트유저")
                val attendance = createAttendance(session, user)

                attendance.attend()

                attendance.status shouldBe AttendanceStatus.ATTEND
            }
        }

        describe("close") {
            it("상태를 ABSENT로 변경한다") {
                val user = createActiveUser("테스트유저")
                val attendance = createAttendance(session, user)

                attendance.close()

                attendance.status shouldBe AttendanceStatus.ABSENT
            }
        }

        describe("isPending") {
            it("상태가 PENDING이면 true를 반환한다") {
                val user = createActiveUser("테스트유저")
                val attendance = createAttendance(session, user)

                attendance.isPending() shouldBe true
            }

            it("상태가 PENDING이 아니면 false를 반환한다") {
                val user = createActiveUser("테스트유저")
                val attendance = createAttendance(session, user)
                attendance.attend()

                attendance.isPending() shouldBe false
            }
        }

        describe("isWrong") {
            it("코드가 일치하지 않으면 true를 반환한다") {
                val user = createActiveUser("테스트유저")
                val attendance = createAttendance(session, user)

                attendance.isWrong(9999) shouldBe true
            }

            it("코드가 일치하면 false를 반환한다") {
                val user = createActiveUser("테스트유저")
                val attendance = createAttendance(session, user)

                attendance.isWrong(1234) shouldBe false
            }
        }
    })
