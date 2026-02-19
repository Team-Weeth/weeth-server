package com.weeth.domain.attendance.domain.entity

import com.weeth.domain.attendance.domain.enums.Status
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createActiveUser
import com.weeth.domain.attendance.fixture.AttendanceTestFixture.createAttendance
import com.weeth.domain.schedule.fixture.ScheduleTestFixture.createMeeting
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class AttendanceTest :
    DescribeSpec({

        describe("attend") {
            it("상태를 ATTEND로 변경한다") {
                val meeting = createMeeting()
                val user = createActiveUser("테스트유저")
                val attendance = createAttendance(meeting, user)
                attendance.init()

                attendance.attend()

                attendance.status shouldBe Status.ATTEND
            }
        }

        describe("close") {
            it("상태를 ABSENT로 변경한다") {
                val meeting = createMeeting()
                val user = createActiveUser("테스트유저")
                val attendance = createAttendance(meeting, user)
                attendance.init()

                attendance.close()

                attendance.status shouldBe Status.ABSENT
            }
        }

        describe("isPending") {
            it("상태가 PENDING이면 true를 반환한다") {
                val meeting = createMeeting()
                val user = createActiveUser("테스트유저")
                val attendance = createAttendance(meeting, user)
                attendance.init()

                attendance.isPending shouldBe true
            }

            it("상태가 PENDING이 아니면 false를 반환한다") {
                val meeting = createMeeting()
                val user = createActiveUser("테스트유저")
                val attendance = createAttendance(meeting, user)
                attendance.init()
                attendance.attend()

                attendance.isPending shouldBe false
            }
        }

        describe("isWrong") {
            it("코드가 일치하지 않으면 true를 반환한다") {
                val meeting = createMeeting()
                val user = createActiveUser("테스트유저")
                val attendance = createAttendance(meeting, user)

                attendance.isWrong(9999) shouldBe true
            }

            it("코드가 일치하면 false를 반환한다") {
                val meeting = createMeeting()
                val user = createActiveUser("테스트유저")
                val attendance = createAttendance(meeting, user)

                attendance.isWrong(1234) shouldBe false
            }
        }

        describe("init") {
            it("상태를 PENDING으로 초기화한다") {
                val meeting = createMeeting()
                val user = createActiveUser("테스트유저")
                val attendance = createAttendance(meeting, user)

                attendance.init()

                attendance.status shouldBe Status.PENDING
            }
        }
    })
