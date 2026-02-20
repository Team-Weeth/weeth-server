package com.weeth.domain.attendance.domain.repository

import com.weeth.config.TestContainersConfig
import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.schedule.domain.entity.Meeting
import com.weeth.domain.schedule.domain.entity.enums.MeetingStatus
import com.weeth.domain.schedule.domain.repository.MeetingRepository
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.enums.Status
import com.weeth.domain.user.domain.repository.UserRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import java.time.LocalDateTime

@DataJpaTest
@Import(TestContainersConfig::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AttendanceRepositoryTest(
    private val attendanceRepository: AttendanceRepository,
    private val meetingRepository: MeetingRepository,
    private val userRepository: UserRepository,
) : DescribeSpec({

        lateinit var meeting: Meeting
        lateinit var activeUser1: User
        lateinit var activeUser2: User

        beforeEach {
            meeting =
                Meeting
                    .builder()
                    .title("1차 정기모임")
                    .start(LocalDateTime.now().minusHours(1))
                    .end(LocalDateTime.now().plusHours(1))
                    .code(1234)
                    .cardinal(1)
                    .meetingStatus(MeetingStatus.OPEN)
                    .build()
            meetingRepository.save(meeting)

            activeUser1 =
                User(
                    name = "이지훈",
                    status = Status.ACTIVE,
                )
            activeUser2 =
                User(
                    name = "이강혁",
                    status = Status.ACTIVE,
                )
            userRepository.saveAll(listOf(activeUser1, activeUser2))
            activeUser1.accept()
            activeUser2.accept()
            userRepository.saveAll(listOf(activeUser1, activeUser2))

            attendanceRepository.save(Attendance(meeting, activeUser1))
            attendanceRepository.save(Attendance(meeting, activeUser2))
        }

        describe("findAllByMeetingAndUserStatus") {
            it("특정 정기모임 + 사용자 상태로 출석 목록 조회") {
                val attendances = attendanceRepository.findAllByMeetingAndUserStatus(meeting, Status.ACTIVE)

                attendances shouldHaveSize 2
                attendances.map { it.user.name } shouldContainExactlyInAnyOrder listOf("이지훈", "이강혁")
            }
        }

        describe("deleteAllByMeeting") {
            it("특정 정기모임의 모든 출석 레코드 삭제") {
                attendanceRepository.deleteAllByMeeting(meeting)

                attendanceRepository.findAll().shouldBeEmpty()
            }
        }
    })
