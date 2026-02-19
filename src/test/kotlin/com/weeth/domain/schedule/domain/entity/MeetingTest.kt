package com.weeth.domain.schedule.domain.entity

import com.weeth.domain.schedule.domain.entity.enums.MeetingStatus
import com.weeth.domain.schedule.fixture.ScheduleTestFixture
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MeetingTest :
    StringSpec({
        "close는 meetingStatus를 CLOSE로 변경한다" {
            val meeting = ScheduleTestFixture.createMeeting(meetingStatus = MeetingStatus.OPEN)

            meeting.close()

            meeting.meetingStatus shouldBe MeetingStatus.CLOSE
        }
    })
