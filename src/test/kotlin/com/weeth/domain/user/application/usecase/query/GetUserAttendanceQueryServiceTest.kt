package com.weeth.domain.user.application.usecase.query

import com.weeth.domain.attendance.domain.enums.AttendanceStatus
import com.weeth.domain.attendance.domain.repository.AttendanceReader
import com.weeth.domain.attendance.fixture.AttendanceTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.session.fixture.SessionTestFixture
import com.weeth.domain.user.application.exception.UserPageNotFoundException
import com.weeth.domain.user.application.mapper.UserAttendanceMapper
import com.weeth.domain.user.fixture.UserTestFixture
import com.weeth.global.common.id.TsidBase62Encoder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.SliceImpl
import java.time.LocalDateTime

class GetUserAttendanceQueryServiceTest :
    DescribeSpec({
        val attendanceReader = mockk<AttendanceReader>()
        val userAttendanceMapper = UserAttendanceMapper()
        val queryService =
            GetUserAttendanceQueryService(
                attendanceReader = attendanceReader,
                userAttendanceMapper = userAttendanceMapper,
            )

        beforeTest {
            clearMocks(attendanceReader)
        }

        describe("getAttendedSessions") {
            it("로그인 사용자의 출석한 세션을 무한스크롤 목록으로 조회한다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                val club = ClubTestFixture.createClub(id = 100L, name = "Leets")
                val member = ClubTestFixture.createClubMember(club = club, user = user)
                val session =
                    SessionTestFixture.createSession(
                        id = 10L,
                        club = club,
                        title = "1차 정기모임",
                        cardinal = 6,
                        start = LocalDateTime.of(2026, 6, 29, 19, 0),
                        end = LocalDateTime.of(2026, 6, 29, 21, 0),
                    )
                val attendance =
                    AttendanceTestFixture
                        .createAttendance(session, member)
                        .also {
                            AttendanceTestFixture.setAttendanceId(it, 1L)
                            it.attend()
                        }
                val pageable = PageRequest.of(0, 10)
                every {
                    attendanceReader.findByUserIdAndStatus(1L, AttendanceStatus.ATTEND, pageable)
                } returns SliceImpl(listOf(attendance), pageable, true)

                val result = queryService.getAttendedSessions(userId = 1L, pageNumber = 0, pageSize = 10)

                result.content shouldHaveSize 1
                result.content[0].attendanceId shouldBe 1L
                result.content[0].clubId shouldBe TsidBase62Encoder.encode(100L)
                result.content[0].clubName shouldBe "Leets"
                result.content[0].sessionId shouldBe 10L
                result.content[0].sessionTitle shouldBe "1차 정기모임"
                result.content[0].cardinal shouldBe 6
                result.content[0].start shouldBe LocalDateTime.of(2026, 6, 29, 19, 0)
                result.content[0].end shouldBe LocalDateTime.of(2026, 6, 29, 21, 0)
                result.content[0].status shouldBe AttendanceStatus.ATTEND
                result.pageNumber shouldBe 0
                result.pageSize shouldBe 10
                result.numberOfElements shouldBe 1
                result.hasNext shouldBe true
            }

            it("pageNumber가 음수면 예외를 던진다") {
                shouldThrow<UserPageNotFoundException> {
                    queryService.getAttendedSessions(userId = 1L, pageNumber = -1, pageSize = 5)
                }
            }

            it("pageSize가 0이면 예외를 던진다") {
                shouldThrow<UserPageNotFoundException> {
                    queryService.getAttendedSessions(userId = 1L, pageNumber = 0, pageSize = 0)
                }
            }

            it("pageSize가 최대값을 초과하면 예외를 던진다") {
                shouldThrow<UserPageNotFoundException> {
                    queryService.getAttendedSessions(userId = 1L, pageNumber = 0, pageSize = 51)
                }
            }
        }
    })
