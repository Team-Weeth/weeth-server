package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.club.application.dto.request.ClubJoinRequest
import com.weeth.domain.club.application.exception.ClubCantJoinException
import com.weeth.domain.club.application.exception.MemberNotActiveException
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.file.application.dto.request.FileSaveRequest
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.enums.FileStatus
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.file.fixture.FileTestFixture
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class ManageClubMemberUseCaseTest :
    DescribeSpec({
        val clubRepository = mockk<ClubRepository>()
        val clubMemberRepository = mockk<ClubMemberRepository>()
        val userReader = mockk<UserReader>()
        val clubMemberPolicy = mockk<ClubMemberPolicy>()
        val fileRepository = mockk<FileRepository>()
        val fileAccessUrlPort = mockk<FileAccessUrlPort>()

        val useCase =
            ManageClubMemberUsecase(
                clubRepository = clubRepository,
                clubMemberRepository = clubMemberRepository,
                userReader = userReader,
                clubMemberPolicy = clubMemberPolicy,
                fileRepository = fileRepository,
                fileAccessUrlPort = fileAccessUrlPort,
            )

        beforeTest {
            clearMocks(
                clubRepository,
                clubMemberRepository,
                userReader,
                clubMemberPolicy,
                fileRepository,
                fileAccessUrlPort,
            )
            every { clubMemberRepository.save(any()) } answers { firstArg() }
            every { fileRepository.save(any()) } answers { firstArg() }
        }

        describe("updateProfileImageUrl") {
            val clubId = 1L
            val userId = 10L
            val request =
                FileSaveRequest(
                    fileName = "profile.png",
                    storageKey = "CLUB_MEMBER_PROFILE/2026-03/00000000-0000-0000-0000-000000000000_profile.png",
                    fileSize = 102400L,
                    contentType = "image/png",
                )

            context("활성 멤버가 프로필 사진을 업데이트할 때") {
                it("기존 파일을 삭제하고 새 파일을 저장한 뒤 URL을 업데이트한다") {
                    val member = ClubMemberTestFixture.createActiveMember(id = 1L)
                    val existingFile =
                        FileTestFixture.createFile(
                            id = 1L,
                            fileName = "old.png",
                            ownerType = FileOwnerType.CLUB_MEMBER_PROFILE,
                            ownerId = member.id,
                        )
                    val resolvedUrl = "https://cdn.example.com/profile.png"

                    every { clubMemberPolicy.getActiveMember(clubId, userId) } returns member
                    every {
                        fileRepository.findAllByOwnerTypeAndOwnerIdAndStatus(
                            FileOwnerType.CLUB_MEMBER_PROFILE,
                            member.id,
                            FileStatus.UPLOADED,
                        )
                    } returns listOf(existingFile)
                    every { fileAccessUrlPort.resolve(any()) } returns resolvedUrl

                    useCase.updateProfileImageUrl(clubId, userId, request)

                    existingFile.status shouldBe FileStatus.DELETED
                    member.profileImageUrl shouldBe resolvedUrl
                    verify(exactly = 1) { fileRepository.save(any()) }
                }
            }

            context("비활성 멤버가 요청할 때") {
                it("MemberNotActiveException을 던진다") {
                    every { clubMemberPolicy.getActiveMember(clubId, userId) } throws MemberNotActiveException()

                    shouldThrow<MemberNotActiveException> {
                        useCase.updateProfileImageUrl(clubId, userId, request)
                    }

                    verify(exactly = 0) { fileRepository.save(any()) }
                }
            }
        }

        describe("deleteProfileImage") {
            val clubId = 1L
            val userId = 10L

            context("활성 멤버가 프로필 사진을 삭제할 때") {
                it("기존 파일을 soft delete하고 URL을 null로 만든다") {
                    val member = ClubMemberTestFixture.createActiveMember(id = 1L)
                    member.updateProfileImageUrl("https://cdn.example.com/profile.png")
                    val existingFile =
                        FileTestFixture.createFile(
                            id = 1L,
                            fileName = "profile.png",
                            ownerType = FileOwnerType.CLUB_MEMBER_PROFILE,
                            ownerId = member.id,
                        )

                    every { clubMemberPolicy.getActiveMember(clubId, userId) } returns member
                    every {
                        fileRepository.findAllByOwnerTypeAndOwnerIdAndStatus(
                            FileOwnerType.CLUB_MEMBER_PROFILE,
                            member.id,
                            FileStatus.UPLOADED,
                        )
                    } returns listOf(existingFile)

                    useCase.deleteProfileImage(clubId, userId)

                    existingFile.status shouldBe FileStatus.DELETED
                    member.profileImageUrl shouldBe null
                }
            }

            context("비활성 멤버가 요청할 때") {
                it("MemberNotActiveException을 던진다") {
                    every { clubMemberPolicy.getActiveMember(clubId, userId) } throws MemberNotActiveException()

                    shouldThrow<MemberNotActiveException> {
                        useCase.deleteProfileImage(clubId, userId)
                    }

                    verify(exactly = 0) {
                        fileRepository.findAllByOwnerTypeAndOwnerIdAndStatus(any(), any(), any())
                    }
                }
            }
        }

        describe("join") {
            context("이미 다른 동아리에서 ACTIVE 상태로 활동 중인 경우") {
                it("MVP 단일 동아리 정책에 따라 가입할 수 없다") {
                    val targetClub = ClubTestFixture.createClub(code = "JOIN-CODE")
                    val anotherClub = ClubTestFixture.createClub()
                    val user = UserTestFixture.createActiveUser1()
                    val anotherClubMember =
                        ClubTestFixture.createClubMember(
                            club = anotherClub,
                            user = user,
                        )

                    every { clubRepository.getClubById(1L) } returns targetClub
                    every { userReader.getByIdWithLock(10L) } returns user
                    every { clubMemberRepository.findByClubIdAndUserId(1L, 10L) } returns null
                    every { clubMemberRepository.findAllByUserId(10L) } returns listOf(anotherClubMember)

                    shouldThrow<ClubCantJoinException> {
                        useCase.join(
                            clubId = 1L,
                            userId = 10L,
                            request = ClubJoinRequest(code = "JOIN-CODE"),
                        )
                    }

                    verify(exactly = 0) { clubMemberRepository.save(any()) }
                }
            }
        }
    })
