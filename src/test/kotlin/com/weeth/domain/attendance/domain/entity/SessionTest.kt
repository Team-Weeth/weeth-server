package com.weeth.domain.attendance.domain.entity

import com.weeth.domain.attendance.domain.entity.enums.SessionStatus
import com.weeth.domain.schedule.fixture.ScheduleTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SessionTest :
    StringSpec({
        "close는 status를 CLOSED로 변경한다" {
            val session = ScheduleTestFixture.createSession(status = SessionStatus.OPEN)

            session.close()

            session.status shouldBe SessionStatus.CLOSED
        }

        "이미 CLOSED 상태에서 close 호출 시 예외가 발생한다" {
            val session = ScheduleTestFixture.createSession(status = SessionStatus.CLOSED)

            shouldThrow<IllegalStateException> {
                session.close()
            }
        }
    })
