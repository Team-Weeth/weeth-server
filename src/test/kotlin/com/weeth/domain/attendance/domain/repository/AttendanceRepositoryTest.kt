package com.weeth.domain.attendance.domain.repository

import com.weeth.config.TestContainersConfig
import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.entity.Session
import com.weeth.domain.attendance.domain.entity.enums.SessionStatus
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
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
) : DescribeSpec({

        lateinit var session: Session
        lateinit var activeUser1: User
        lateinit var activeUser2: User

        beforeEach {
            session =
                Session(
                    title = "1차 정기모임",
                    start = LocalDateTime.now().minusHours(1),
                    end = LocalDateTime.now().plusHours(1),
                    code = 1234,
                    cardinal = 1,
                    status = SessionStatus.OPEN,
                )
            sessionRepository.save(session)

            activeUser1 =
                User
                    .builder()
                    .name("이지훈")
                    .status(Status.ACTIVE)
                    .build()
            activeUser2 =
                User
                    .builder()
                    .name("이강혁")
                    .status(Status.ACTIVE)
                    .build()
            userRepository.saveAll(listOf(activeUser1, activeUser2))
            activeUser1.accept()
            activeUser2.accept()
            userRepository.saveAll(listOf(activeUser1, activeUser2))

            attendanceRepository.save(Attendance.create(session, activeUser1))
            attendanceRepository.save(Attendance.create(session, activeUser2))
        }

        describe("findAllBySessionAndUserStatus") {
            it("특정 세션 + 사용자 상태로 출석 목록 조회") {
                val attendances = attendanceRepository.findAllBySessionAndUserStatus(session, Status.ACTIVE)

                attendances shouldHaveSize 2
                attendances.map { it.user.name } shouldContainExactlyInAnyOrder listOf("이지훈", "이강혁")
            }
        }

        describe("deleteAllBySession") {
            it("특정 세션의 모든 출석 레코드 삭제") {
                attendanceRepository.deleteAllBySession(session)

                attendanceRepository.findAll().shouldBeEmpty()
            }
        }
    })
