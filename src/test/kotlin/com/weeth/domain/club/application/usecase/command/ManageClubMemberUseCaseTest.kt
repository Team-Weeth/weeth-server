package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.cardinal.fixture.CardinalTestFixture
import com.weeth.domain.club.application.dto.request.ClubJoinRequest
import com.weeth.domain.club.application.dto.request.ClubMemberCardinalSetRequest
import com.weeth.domain.club.application.dto.request.UpdateMemberProfileRequest
import com.weeth.domain.club.application.exception.CannotLeaveAsLeadException
import com.weeth.domain.club.application.exception.CardinalAlreadySetException
import com.weeth.domain.club.application.exception.ClubJoinLimitExceededException
import com.weeth.domain.club.application.exception.ClubMemberNotFoundException
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberCardinalRepository
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.domain.service.ClubJoinPolicy
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.file.application.dto.request.FileSaveRequest
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.enums.FileStatus
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.file.fixture.FileTestFixture
import com.weeth.domain.session.domain.repository.SessionReader
import com.weeth.domain.session.fixture.SessionTestFixture
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class ManageClubMemberUseCaseTest :
    DescribeSpec({
        val clubRepository = mockk<ClubRepository>()
        val clubMemberRepository = mockk<ClubMemberRepository>()
        val clubMemberCardinalRepository = mockk<ClubMemberCardinalRepository>(relaxed = true)
        val cardinalReader = mockk<CardinalReader>()
        val sessionReader = mockk<SessionReader>()
        val attendanceRepository = mockk<AttendanceRepository>(relaxed = true)
        val userReader = mockk<UserReader>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val clubJoinPolicy = mockk<ClubJoinPolicy>()
        val fileRepository = mockk<FileRepository>()
        val fileAccessUrlPort = mockk<FileAccessUrlPort>()

        val useCase =
            ManageClubMemberUsecase(
                clubRepository = clubRepository,
                clubMemberRepository = clubMemberRepository,
                clubMemberCardinalRepository = clubMemberCardinalRepository,
                cardinalReader = cardinalReader,
                sessionReader = sessionReader,
                attendanceRepository = attendanceRepository,
                userReader = userReader,
                clubMemberPolicy = clubMemberPolicy,
                clubJoinPolicy = clubJoinPolicy,
                fileRepository = fileRepository,
                fileAccessUrlPort = fileAccessUrlPort,
            )

        beforeTest {
            clearMocks(
                clubRepository,
                clubMemberRepository,
                clubMemberCardinalRepository,
                cardinalReader,
                sessionReader,
                attendanceRepository,
                userReader,
                clubMemberPolicy,
                clubJoinPolicy,
                fileRepository,
                fileAccessUrlPort,
            )
            every { clubMemberRepository.save(any()) } answers { firstArg() }
            every { fileRepository.save(any()) } answers { firstArg() }
        }

        describe("updateProfile") {
            val userId = 10L
            val profileImageRequest =
                FileSaveRequest(
                    fileName = "profile.png",
                    storageKey = "CLUB_MEMBER_PROFILE/2026-03/00000000-0000-0000-0000-000000000000_profile.png",
                    fileSize = 102400L,
                    contentType = "image/png",
                )

            context("프로필 사진만 변경할 때") {
                it("모든 활성 ClubMember의 기존 파일을 soft delete하고 새 파일로 URL을 업데이트한다") {
                    val member1 = ClubMemberTestFixture.createActiveMember(id = 1L)
                    val member2 = ClubMemberTestFixture.createActiveMember(id = 2L)
                    val existingFile =
                        FileTestFixture.createFile(
                            id = 1L,
                            fileName = "old.png",
                            ownerType = FileOwnerType.CLUB_MEMBER_PROFILE,
                            ownerId = userId,
                        )
                    every { clubMemberRepository.findActiveByUserId(userId) } returns listOf(member1, member2)
                    every {
                        fileRepository.findAllByOwnerTypeAndOwnerIdAndStatus(
                            FileOwnerType.CLUB_MEMBER_PROFILE,
                            userId,
                            FileStatus.UPLOADED,
                        )
                    } returns listOf(existingFile)
                    useCase.updateProfile(userId, UpdateMemberProfileRequest(profileImage = profileImageRequest))

                    existingFile.status shouldBe FileStatus.DELETED
                    member1.profileImageStorageKey shouldBe profileImageRequest.storageKey
                    member2.profileImageStorageKey shouldBe profileImageRequest.storageKey
                    verify(exactly = 1) { fileRepository.save(any()) }
                }
            }

            context("bio만 변경할 때") {
                it("모든 활성 ClubMember의 bio를 업데이트하고 파일 관련 작업은 수행하지 않는다") {
                    val member1 = ClubMemberTestFixture.createActiveMember(id = 1L)
                    val member2 = ClubMemberTestFixture.createActiveMember(id = 2L)

                    every { clubMemberRepository.findActiveByUserId(userId) } returns listOf(member1, member2)

                    useCase.updateProfile(userId, UpdateMemberProfileRequest(bio = "안녕하세요!"))

                    member1.bio shouldBe "안녕하세요!"
                    member2.bio shouldBe "안녕하세요!"
                    verify(exactly = 0) { fileRepository.findAllByOwnerTypeAndOwnerIdAndStatus(any(), any(), any()) }
                    verify(exactly = 0) { fileRepository.save(any()) }
                }
            }

            context("bio를 빈 문자열로 보낼 때") {
                it("모든 활성 ClubMember의 bio가 null로 저장된다") {
                    val member1 = ClubMemberTestFixture.createActiveMember(id = 1L)
                    val member2 = ClubMemberTestFixture.createActiveMember(id = 2L)

                    every { clubMemberRepository.findActiveByUserId(userId) } returns listOf(member1, member2)

                    useCase.updateProfile(userId, UpdateMemberProfileRequest(bio = ""))

                    member1.bio shouldBe null
                    member2.bio shouldBe null
                }
            }

            context("활성 동아리 멤버십이 없을 때") {
                it("ClubMemberNotFoundException을 던진다") {
                    every { clubMemberRepository.findActiveByUserId(userId) } returns emptyList()

                    shouldThrow<ClubMemberNotFoundException> {
                        useCase.updateProfile(userId, UpdateMemberProfileRequest(bio = "안녕하세요!"))
                    }
                }
            }
        }

        describe("deleteProfileImage") {
            val userId = 10L

            context("활성 멤버가 프로필 사진을 삭제할 때") {
                it("모든 활성 ClubMember의 파일을 soft delete하고 URL을 null로 만든다") {
                    val member1 = ClubMemberTestFixture.createActiveMember(id = 1L)
                    val member2 = ClubMemberTestFixture.createActiveMember(id = 2L)
                    member1.updateProfileImageUrl("CLUB_MEMBER_PROFILE/2026-02/uuid_profile.png")
                    member2.updateProfileImageUrl("CLUB_MEMBER_PROFILE/2026-02/uuid_profile.png")
                    val existingFile =
                        FileTestFixture.createFile(
                            id = 1L,
                            fileName = "profile.png",
                            ownerType = FileOwnerType.CLUB_MEMBER_PROFILE,
                            ownerId = userId,
                        )

                    every { clubMemberRepository.findActiveByUserId(userId) } returns listOf(member1, member2)
                    every {
                        fileRepository.findAllByOwnerTypeAndOwnerIdAndStatus(
                            FileOwnerType.CLUB_MEMBER_PROFILE,
                            userId,
                            FileStatus.UPLOADED,
                        )
                    } returns listOf(existingFile)

                    useCase.deleteProfileImage(userId)

                    existingFile.status shouldBe FileStatus.DELETED
                    member1.profileImageStorageKey shouldBe null
                    member2.profileImageStorageKey shouldBe null
                }
            }

            context("활성 동아리 멤버십이 없을 때") {
                it("ClubMemberNotFoundException을 던진다") {
                    every { clubMemberRepository.findActiveByUserId(userId) } returns emptyList()

                    shouldThrow<ClubMemberNotFoundException> {
                        useCase.deleteProfileImage(userId)
                    }
                }
            }
        }

        describe("setInitialCardinals") {
            val club = ClubTestFixture.createClub()
            val member = ClubMemberTestFixture.createActiveMember(club = club)

            context("복수 기수를 최초 설정하는 경우") {
                it("요청 기수 수만큼 ClubMemberCardinal이 저장되고, 각 기수의 세션에 출석이 초기화된다") {
                    val cardinal30 =
                        CardinalTestFixture.createCardinal(
                            id = 1L,
                            club = club,
                            cardinalNumber = 30,
                        )
                    val cardinal31 =
                        CardinalTestFixture.createCardinal(
                            id = 2L,
                            club = club,
                            cardinalNumber = 31,
                        )
                    val session30 = SessionTestFixture.createSession(club = club, cardinal = 30)
                    val session31 = SessionTestFixture.createSession(club = club, cardinal = 31)

                    every { clubMemberPolicy.getActiveMemberWithLock(1L, 10L) } returns member
                    every { clubMemberCardinalRepository.existsByClubMember(member) } returns false
                    every { cardinalReader.findByClubIdAndCardinalNumber(1L, 30) } returns cardinal30
                    every { cardinalReader.findByClubIdAndCardinalNumber(1L, 31) } returns cardinal31
                    every { clubMemberCardinalRepository.saveAll(any<List<ClubMemberCardinal>>()) } answers
                        { firstArg() }
                    every {
                        sessionReader.findAllByClubIdAndCardinalIn(1L, listOf(30, 31))
                    } returns listOf(session30, session31)

                    useCase.setInitialCardinals(1L, 10L, ClubMemberCardinalSetRequest(cardinals = listOf(30, 31)))

                    verify(exactly = 1) {
                        clubMemberCardinalRepository.saveAll(
                            match<List<ClubMemberCardinal>> {
                                it.size ==
                                    2
                            },
                        )
                    }
                    verify(exactly = 1) {
                        attendanceRepository.saveAll(
                            match<List<com.weeth.domain.attendance.domain.entity.Attendance>> {
                                it.size ==
                                    2
                            },
                        )
                    }
                }
            }

            context("세션이 없는 기수를 설정하는 경우") {
                it("ClubMemberCardinal만 저장되고 출석은 초기화되지 않는다") {
                    val cardinal =
                        CardinalTestFixture.createCardinal(
                            id = 1L,
                            club = club,
                            cardinalNumber = 30,
                        )

                    every { clubMemberPolicy.getActiveMemberWithLock(1L, 10L) } returns member
                    every { clubMemberCardinalRepository.existsByClubMember(member) } returns false
                    every { cardinalReader.findByClubIdAndCardinalNumber(1L, 30) } returns cardinal
                    every { clubMemberCardinalRepository.saveAll(any<List<ClubMemberCardinal>>()) } answers
                        { firstArg() }
                    every { sessionReader.findAllByClubIdAndCardinalIn(1L, listOf(30)) } returns emptyList()

                    useCase.setInitialCardinals(1L, 10L, ClubMemberCardinalSetRequest(cardinals = listOf(30)))

                    verify(exactly = 1) {
                        clubMemberCardinalRepository.saveAll(
                            match<List<ClubMemberCardinal>> {
                                it.size ==
                                    1
                            },
                        )
                    }
                    verify(
                        exactly = 0,
                    ) {
                        attendanceRepository.saveAll(
                            any<List<com.weeth.domain.attendance.domain.entity.Attendance>>(),
                        )
                    }
                }
            }

            context("요청에 중복 기수가 포함된 경우") {
                it("중복을 제거하고 1개만 저장한다") {
                    val cardinal =
                        CardinalTestFixture.createCardinal(
                            id = 1L,
                            club = club,
                            cardinalNumber = 30,
                        )

                    every { clubMemberPolicy.getActiveMemberWithLock(1L, 10L) } returns member
                    every { clubMemberCardinalRepository.existsByClubMember(member) } returns false
                    every { cardinalReader.findByClubIdAndCardinalNumber(1L, 30) } returns cardinal
                    every { clubMemberCardinalRepository.saveAll(any<List<ClubMemberCardinal>>()) } answers
                        { firstArg() }
                    every { sessionReader.findAllByClubIdAndCardinalIn(1L, listOf(30)) } returns emptyList()

                    useCase.setInitialCardinals(1L, 10L, ClubMemberCardinalSetRequest(cardinals = listOf(30, 30)))

                    verify(exactly = 1) {
                        clubMemberCardinalRepository.saveAll(
                            match<List<ClubMemberCardinal>> {
                                it.size ==
                                    1
                            },
                        )
                    }
                }
            }

            context("이미 기수가 설정된 멤버가 재설정을 시도하는 경우") {
                it("CardinalAlreadySetException이 발생한다") {
                    every { clubMemberPolicy.getActiveMemberWithLock(1L, 10L) } returns member
                    every { clubMemberCardinalRepository.existsByClubMember(member) } returns true

                    shouldThrow<CardinalAlreadySetException> {
                        useCase.setInitialCardinals(1L, 10L, ClubMemberCardinalSetRequest(cardinals = listOf(31)))
                    }

                    verify(exactly = 0) { clubMemberCardinalRepository.saveAll(any<List<ClubMemberCardinal>>()) }
                }
            }

            context("존재하지 않는 기수를 요청하는 경우") {
                it("CardinalNotFoundException이 발생한다") {
                    every { clubMemberPolicy.getActiveMemberWithLock(1L, 10L) } returns member
                    every { clubMemberCardinalRepository.existsByClubMember(member) } returns false
                    every { cardinalReader.findByClubIdAndCardinalNumber(1L, 99) } returns null

                    shouldThrow<CardinalNotFoundException> {
                        useCase.setInitialCardinals(1L, 10L, ClubMemberCardinalSetRequest(cardinals = listOf(99)))
                    }

                    verify(exactly = 0) { clubMemberCardinalRepository.saveAll(any<List<ClubMemberCardinal>>()) }
                }
            }
        }

        describe("leave") {
            it("LEAD 멤버가 탈퇴를 시도하면 예외가 발생한다") {
                val leadMember = ClubMemberTestFixture.createLeadMember()
                every { clubMemberPolicy.getActiveMemberWithLock(1L, 10L) } returns leadMember

                shouldThrow<CannotLeaveAsLeadException> {
                    useCase.leave(1L, 10L)
                }
            }

            it("일반 멤버가 탈퇴하면 LEFT 상태로 전환된다") {
                val member = ClubMemberTestFixture.createActiveMember()
                every { clubMemberPolicy.getActiveMemberWithLock(1L, 10L) } returns member

                useCase.leave(1L, 10L)

                member.memberStatus shouldBe MemberStatus.LEFT
            }
        }

        describe("join") {
            context("이미 USER로 1개 동아리에 가입한 사용자가 가입 시도하는 경우") {
                it("ClubJoinLimitExceededException이 발생한다") {
                    val targetClub = ClubTestFixture.createClub(code = "JOIN-CODE")
                    val user = UserTestFixture.createActiveUser1()

                    every { clubRepository.getClubById(1L) } returns targetClub
                    every { userReader.getByIdWithLock(10L) } returns user
                    every { clubMemberRepository.findByClubIdAndUserId(1L, 10L) } returns null
                    every { clubJoinPolicy.validateJoinLimit(10L) } throws ClubJoinLimitExceededException()

                    shouldThrow<ClubJoinLimitExceededException> {
                        useCase.join(
                            clubId = 1L,
                            userId = 10L,
                            request = ClubJoinRequest(code = "JOIN-CODE"),
                        )
                    }

                    verify(exactly = 0) { clubMemberRepository.save(any()) }
                }
            }

            context("LEAD로 1개 동아리를 생성한 사용자가 USER로 가입 시도하는 경우") {
                it("역할이 다르므로 가입에 성공한다") {
                    val targetClub = ClubTestFixture.createClub(code = "JOIN-CODE")
                    val user = UserTestFixture.createActiveUser1()

                    every { clubRepository.getClubById(1L) } returns targetClub
                    every { userReader.getByIdWithLock(10L) } returns user
                    every { clubMemberRepository.findByClubIdAndUserId(1L, 10L) } returns null
                    justRun { clubJoinPolicy.validateJoinLimit(10L) }

                    useCase.join(
                        clubId = 1L,
                        userId = 10L,
                        request = ClubJoinRequest(code = "JOIN-CODE"),
                    )

                    verify(exactly = 1) { clubMemberRepository.save(any()) }
                }
            }
        }
    })
