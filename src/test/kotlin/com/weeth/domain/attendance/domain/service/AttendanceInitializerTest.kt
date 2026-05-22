package com.weeth.domain.attendance.domain.service

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.session.domain.repository.SessionReader
import com.weeth.domain.session.fixture.SessionTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class AttendanceInitializerTest :
    DescribeSpec({
        val sessionReader = mockk<SessionReader>()
        val attendanceRepository = mockk<AttendanceRepository>(relaxed = true)
        val initializer = AttendanceInitializer(sessionReader, attendanceRepository)

        beforeTest {
            clearMocks(sessionReader, attendanceRepository)
            every { attendanceRepository.saveAll(any<List<Attendance>>()) } answers { firstArg() }
        }

        describe("initializeForMemberCardinals") {
            it("기수에 속한 세션별로 멤버 출석을 생성한다") {
                val club = ClubTestFixture.createClub(id = 1L)
                val member = ClubMemberTestFixture.createActiveMember(club = club)
                val cardinal30 = CardinalTestFixture.createCardinal(club = club, cardinalNumber = 30)
                val cardinal31 = CardinalTestFixture.createCardinal(club = club, cardinalNumber = 31)
                val session30 = SessionTestFixture.createSession(club = club, cardinal = 30)
                val session31 = SessionTestFixture.createSession(club = club, cardinal = 31)

                every { sessionReader.findAllByClubIdAndCardinalIn(1L, listOf(30, 31)) } returns
                    listOf(session30, session31)

                initializer.initializeForMemberCardinals(1L, member, listOf(cardinal30, cardinal31))

                verify(exactly = 1) {
                    attendanceRepository.saveAll(
                        match<List<Attendance>> { attendances ->
                            attendances.size == 2 &&
                                attendances.all { it.clubMember == member } &&
                                attendances.map { it.session } == listOf(session30, session31)
                        },
                    )
                }
            }

            it("세션이 없으면 출석을 저장하지 않는다") {
                val club = ClubTestFixture.createClub(id = 1L)
                val member = ClubMemberTestFixture.createActiveMember(club = club)
                val cardinal = CardinalTestFixture.createCardinal(club = club, cardinalNumber = 30)

                every { sessionReader.findAllByClubIdAndCardinalIn(1L, listOf(30)) } returns emptyList()

                initializer.initializeForMemberCardinals(1L, member, listOf(cardinal))

                verify(exactly = 0) { attendanceRepository.saveAll(any<List<Attendance>>()) }
            }

            it("중복 기수는 제거하고 세션을 조회한다") {
                val club = ClubTestFixture.createClub(id = 1L)
                val member = ClubMemberTestFixture.createActiveMember(club = club)
                val cardinal = CardinalTestFixture.createCardinal(club = club, cardinalNumber = 30)

                every { sessionReader.findAllByClubIdAndCardinalIn(1L, listOf(30)) } returns emptyList()

                initializer.initializeForMemberCardinals(1L, member, listOf(cardinal, cardinal))

                verify(exactly = 1) { sessionReader.findAllByClubIdAndCardinalIn(1L, listOf(30)) }
            }
        }
    })
