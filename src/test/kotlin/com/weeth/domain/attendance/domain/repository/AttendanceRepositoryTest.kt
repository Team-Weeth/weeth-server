package com.weeth.domain.attendance.domain.repository

import com.weeth.config.TestContainersConfig
import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.session.domain.entity.Session
import com.weeth.domain.session.domain.enums.SessionStatus
import com.weeth.domain.session.domain.repository.SessionRepository
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.repository.UserRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
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
    private val clubRepository: ClubRepository,
    private val clubMemberRepository: ClubMemberRepository,
) : DescribeSpec({

        lateinit var session: Session
        lateinit var activeUser1: User
        lateinit var activeUser2: User
        lateinit var activeMember1: ClubMember
        lateinit var activeMember2: ClubMember

        beforeEach {
            val club = clubRepository.save(ClubTestFixture.createClub())

            session =
                Session(
                    club = club,
                    title = "1차 정기모임",
                    start = LocalDateTime.now().minusHours(1),
                    end = LocalDateTime.now().plusHours(1),
                    code = 1234,
                    cardinal = 1,
                    status = SessionStatus.OPEN,
                )
            sessionRepository.save(session)

            activeUser1 =
                User.create(
                    name = "이지훈",
                    email = "lee.jihoon@test.com",
                    studentId = "",
                    tel = "",
                    department = "",
                )
            activeUser2 =
                User.create(
                    name = "이강혁",
                    email = "lee.ganghyuk@test.com",
                    studentId = "",
                    tel = "",
                    department = "",
                )
            userRepository.saveAll(listOf(activeUser1, activeUser2))
            activeUser1.accept()
            activeUser2.accept()
            userRepository.saveAll(listOf(activeUser1, activeUser2))

            activeMember1 =
                clubMemberRepository.save(
                    ClubMember(
                        club = club,
                        user = activeUser1,
                        memberStatus = MemberStatus.ACTIVE,
                    ),
                )
            activeMember2 =
                clubMemberRepository.save(
                    ClubMember(
                        club = club,
                        user = activeUser2,
                        memberStatus = MemberStatus.ACTIVE,
                    ),
                )

            attendanceRepository.save(Attendance.create(session, activeMember1))
            attendanceRepository.save(Attendance.create(session, activeMember2))
        }

        describe("findAllBySessionAndClubMemberMemberStatus") {
            it("특정 세션 + 멤버 상태로 출석 목록 조회") {
                val attendances =
                    attendanceRepository.findAllBySessionAndClubMemberMemberStatus(
                        session,
                        MemberStatus.ACTIVE,
                    )

                attendances shouldHaveSize 2
                attendances.map { it.clubMember.user.name } shouldContainExactlyInAnyOrder listOf("이지훈", "이강혁")
            }
        }

        describe("기수별 출석 조회와 일괄 집계") {
            it("선택 기수와 페이지 멤버 및 동아리만 집계하고 PENDING을 제외한다") {
                val club = activeMember1.club
                attendanceRepository.findAllBySession(session).first { it.clubMember.id == activeMember1.id }.attend()
                val oldSession =
                    sessionRepository.save(
                        Session(
                            club = club,
                            title = "과거",
                            cardinal = 7,
                            start = session.start,
                            end = session.end,
                            code = 1234,
                        ),
                    )
                val absentSession =
                    sessionRepository.save(
                        Session(
                            club = club,
                            title = "결석",
                            cardinal = 7,
                            start = session.start,
                            end = session.end,
                            code = 1234,
                        ),
                    )
                val pendingSession =
                    sessionRepository.save(
                        Session(
                            club = club,
                            title = "미결",
                            cardinal = 7,
                            start = session.start,
                            end = session.end,
                            code = 1234,
                        ),
                    )
                attendanceRepository.save(Attendance.create(oldSession, activeMember1).also { it.attend() })
                attendanceRepository.save(Attendance.create(absentSession, activeMember1).also { it.absent() })
                attendanceRepository.save(Attendance.create(pendingSession, activeMember1))
                attendanceRepository.save(Attendance.create(oldSession, activeMember2).also { it.attend() })
                val otherClub = clubRepository.save(ClubTestFixture.createClub(code = "OTHER", name = "다른 동아리"))
                val otherMember = clubMemberRepository.save(ClubMember(club = otherClub, user = activeUser1))
                val otherSession =
                    sessionRepository.save(
                        Session(
                            club = otherClub,
                            title = "다른 동아리",
                            cardinal = 7,
                            start = session.start,
                            end = session.end,
                            code = 1234,
                        ),
                    )
                attendanceRepository.save(Attendance.create(otherSession, otherMember).also { it.attend() })
                attendanceRepository.flush()

                val rows =
                    attendanceRepository.countByClubIdAndMemberIdsAndCardinal(
                        club.id,
                        listOf(activeMember1.id, otherMember.id),
                        7,
                    )
                rows shouldHaveSize 1
                rows.single().clubMemberId shouldBe activeMember1.id
                rows.single().attendanceCount shouldBe 1L
                rows.single().absenceCount shouldBe 1L
                val records = attendanceRepository.findAllByClubMemberIdAndCardinal(activeMember1.id, 7)
                records shouldHaveSize 3
                records.map { it.session.cardinal }.distinct() shouldBe listOf(7)
                attendanceRepository
                    .countByClubIdAndMemberIdsAndCardinal(
                        club.id,
                        listOf(activeMember1.id),
                        99,
                    ).shouldBeEmpty()
            }
        }

        describe("deleteAllBySession") {
            it("특정 세션의 모든 출석 레코드 삭제") {
                attendanceRepository.deleteAllBySession(session)

                attendanceRepository.findAll().shouldBeEmpty()
            }
        }
    })
