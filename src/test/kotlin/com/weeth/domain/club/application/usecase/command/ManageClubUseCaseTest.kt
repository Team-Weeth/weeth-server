package com.weeth.domain.club.application.usecase.command

import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.domain.enums.CardinalStatus
import com.weeth.domain.cardinal.domain.repository.CardinalRepository
import com.weeth.domain.club.application.dto.request.ClubCreateRequest
import com.weeth.domain.club.application.dto.request.ClubUpdateRequest
import com.weeth.domain.club.application.exception.ClubCreateLimitExceededException
import com.weeth.domain.club.domain.entity.ClubMemberCardinal
import com.weeth.domain.club.domain.enums.PrimaryContact
import com.weeth.domain.club.domain.repository.ClubMemberCardinalRepository
import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.domain.repository.ClubRepository
import com.weeth.domain.club.domain.service.ClubJoinPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.club.domain.vo.ClubContact
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.file.application.dto.request.FileSaveRequest
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.enums.FileStatus
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class ManageClubUseCaseTest :
    DescribeSpec({
        val clubRepository = mockk<ClubRepository>()
        val clubMemberRepository = mockk<ClubMemberRepository>()
        val cardinalRepository = mockk<CardinalRepository>()
        val clubMemberCardinalRepository = mockk<ClubMemberCardinalRepository>()
        val userReader = mockk<UserReader>()
        val clubJoinPolicy = mockk<ClubJoinPolicy>()
        val clubPermissionPolicy = mockk<ClubPermissionPolicy>()
        val fileRepository = mockk<FileRepository>()
        val useCase =
            ManageClubUseCase(
                clubRepository,
                clubMemberRepository,
                cardinalRepository,
                clubMemberCardinalRepository,
                userReader,
                clubJoinPolicy,
                clubPermissionPolicy,
                fileRepository,
            )
        val adminMember =
            com.weeth.domain.club.fixture.ClubMemberTestFixture
                .createAdminMember()

        beforeTest {
            clearMocks(
                clubRepository,
                clubMemberRepository,
                cardinalRepository,
                clubMemberCardinalRepository,
                userReader,
                clubJoinPolicy,
                clubPermissionPolicy,
                fileRepository,
            )
            every { clubRepository.save(any()) } answers { firstArg() }
            every { clubMemberRepository.save(any()) } answers { firstArg() }
            every { cardinalRepository.saveAll(any<List<Cardinal>>()) } answers { firstArg() }
            every { clubMemberCardinalRepository.save(any()) } answers { firstArg() }
            every { clubJoinPolicy.validateCreateLimit(any()) } just Runs
            every { fileRepository.save(any<File>()) } answers { firstArg() }
            every {
                fileRepository.findAllByOwnerTypeAndOwnerIdAndStatus(any(), any(), any())
            } returns emptyList()
        }

        describe("create") {
            val user = UserTestFixture.createActiveUser1()

            context("N기 동아리를 개설하는 경우") {
                it("1기부터 N기까지 Cardinal이 생성되며, 마지막 기수만 IN_PROGRESS이다") {
                    val cardinalSlot = slot<List<Cardinal>>()
                    every { userReader.getByIdWithLock(10L) } returns user
                    every { cardinalRepository.saveAll(capture(cardinalSlot)) } answers { firstArg() }

                    useCase.create(
                        10L,
                        ClubCreateRequest(
                            name = "테스트",
                            schoolName = "가천대",
                            currentCardinal = 3,
                            contactPhoneNumber = "01000000000",
                            primaryContact = PrimaryContact.PHONE,
                            contactEmail = "test@example.com",
                        ),
                    )

                    val cardinals = cardinalSlot.captured
                    cardinals.size shouldBe 3
                    cardinals[0].cardinalNumber shouldBe 1
                    cardinals[0].status shouldBe CardinalStatus.DONE
                    cardinals[1].cardinalNumber shouldBe 2
                    cardinals[1].status shouldBe CardinalStatus.DONE
                    cardinals[2].cardinalNumber shouldBe 3
                    cardinals[2].status shouldBe CardinalStatus.IN_PROGRESS
                }

                it("LEAD 멤버가 최신 기수에 ClubMemberCardinal로 배정된다") {
                    every { userReader.getByIdWithLock(10L) } returns user

                    useCase.create(
                        10L,
                        ClubCreateRequest(
                            name = "테스트",
                            schoolName = "가천대",
                            currentCardinal = 3,
                            contactPhoneNumber = "01000000000",
                            primaryContact = PrimaryContact.PHONE,
                            contactEmail = "test@example.com",
                        ),
                    )

                    verify(exactly = 1) { clubMemberCardinalRepository.save(any<ClubMemberCardinal>()) }
                }

                it("1기만 있는 동아리 개설 시 Cardinal 1개가 IN_PROGRESS로 생성된다") {
                    val cardinalSlot = slot<List<Cardinal>>()
                    every { userReader.getByIdWithLock(10L) } returns user
                    every { cardinalRepository.saveAll(capture(cardinalSlot)) } answers { firstArg() }

                    useCase.create(
                        10L,
                        ClubCreateRequest(
                            name = "테스트",
                            schoolName = "가천대",
                            currentCardinal = 1,
                            contactPhoneNumber = "01000000000",
                            primaryContact = PrimaryContact.PHONE,
                            contactEmail = "test@example.com",
                        ),
                    )

                    val cardinals = cardinalSlot.captured
                    cardinals.size shouldBe 1
                    cardinals[0].cardinalNumber shouldBe 1
                    cardinals[0].status shouldBe CardinalStatus.IN_PROGRESS
                }
            }

            context("이미지와 함께 동아리를 개설하는 경우") {
                it("프로필/배경 이미지에 대한 File 레코드가 각각 생성된다") {
                    every { userReader.getByIdWithLock(10L) } returns user

                    useCase.create(
                        10L,
                        ClubCreateRequest(
                            name = "테스트",
                            schoolName = "가천대",
                            currentCardinal = 1,
                            contactPhoneNumber = "01000000000",
                            primaryContact = PrimaryContact.PHONE,
                            profileImage =
                                FileSaveRequest(
                                    fileName = "profile.png",
                                    storageKey = "CLUB_PROFILE/2026-03/550e8400-e29b-41d4-a716-446655440000_pf.png",
                                    fileSize = 1024,
                                    contentType = "image/png",
                                ),
                            backgroundImage =
                                FileSaveRequest(
                                    fileName = "bg.png",
                                    storageKey = "CLUB_BACKGROUND/2026-03/550e8400-e29b-41d4-a716-446655440001_bg.png",
                                    fileSize = 2048,
                                    contentType = "image/png",
                                ),
                        ),
                    )

                    verify(exactly = 2) { fileRepository.save(any<File>()) }
                }

                it("이미지 없이 개설하면 File 레코드가 생성되지 않는다") {
                    every { userReader.getByIdWithLock(10L) } returns user

                    useCase.create(
                        10L,
                        ClubCreateRequest(
                            name = "테스트",
                            schoolName = "가천대",
                            currentCardinal = 1,
                            contactPhoneNumber = "01000000000",
                            primaryContact = PrimaryContact.PHONE,
                        ),
                    )

                    verify(exactly = 0) { fileRepository.save(any<File>()) }
                }
            }

            context("이미 LEAD로 1개 동아리를 생성한 사용자가 생성 시도하는 경우") {
                it("ClubCreateLimitExceededException이 발생하고, 이후 로직이 실행되지 않는다") {
                    every { userReader.getByIdWithLock(13L) } returns user
                    every { clubJoinPolicy.validateCreateLimit(13L) } throws ClubCreateLimitExceededException()

                    shouldThrow<ClubCreateLimitExceededException> {
                        useCase.create(
                            13L,
                            ClubCreateRequest(
                                name = "새 동아리",
                                schoolName = "가천대학교",
                                description = "소개",
                                currentCardinal = 3,
                                contactPhoneNumber = "01000000000",
                                primaryContact = PrimaryContact.PHONE,
                            ),
                        )
                    }

                    verify(exactly = 1) { userReader.getByIdWithLock(13L) }
                    verify(exactly = 1) { clubJoinPolicy.validateCreateLimit(13L) }
                    verify(exactly = 0) { clubRepository.save(any()) }
                    verify(exactly = 0) { clubMemberRepository.save(any()) }
                    verify(exactly = 0) { cardinalRepository.saveAll(any<List<Cardinal>>()) }
                }
            }
        }

        describe("update") {
            it("null 필드는 유지하고 전달된 필드만 수정한다") {
                val club =
                    ClubTestFixture.createClub(
                        name = "기존 동아리",
                        schoolName = "가천대학교",
                        description = "기존 소개",
                        clubContact =
                            ClubContact.from(
                                email = "club@example.com",
                                phoneNumber = "01011112222",
                                primaryContact = PrimaryContact.PHONE,
                            ),
                    )
                club.update(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "CLUB_PROFILE/2026-02/uuid_profile.png",
                    "CLUB_BACKGROUND/2026-02/uuid_background.png",
                )

                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubRepository.getClubById(1L) } returns club

                useCase.update(
                    1L,
                    10L,
                    ClubUpdateRequest(
                        schoolName = "연세대학교",
                        contactPhoneNumber = "01099998888",
                    ),
                )

                club.name shouldBe "기존 동아리"
                club.schoolName shouldBe "연세대학교"
                club.description shouldBe "기존 소개"
                club.clubContact.email shouldBe "club@example.com"
                club.clubContact.phoneNumber shouldBe "01099998888"
                club.profileImageStorageKey shouldBe "CLUB_PROFILE/2026-02/uuid_profile.png"
                club.backgroundImageStorageKey shouldBe "CLUB_BACKGROUND/2026-02/uuid_background.png"
            }

            it("프로필 이미지를 변경하면 기존 File이 DELETED 처리되고 새 File이 생성된다") {
                val existingFile = mockk<File>(relaxed = true)
                val club =
                    ClubTestFixture.createClub(
                        clubContact =
                            ClubContact.from(
                                email = "club@example.com",
                                phoneNumber = "01011112222",
                                primaryContact = PrimaryContact.PHONE,
                            ),
                    )

                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubRepository.getClubById(1L) } returns club
                every {
                    fileRepository.findAllByOwnerTypeAndOwnerIdAndStatus(
                        FileOwnerType.CLUB_PROFILE,
                        1L,
                        FileStatus.UPLOADED,
                    )
                } returns listOf(existingFile)

                useCase.update(
                    1L,
                    10L,
                    ClubUpdateRequest(
                        profileImage =
                            FileSaveRequest(
                                fileName = "new_profile.png",
                                storageKey = "CLUB_PROFILE/2026-03/550e8400-e29b-41d4-a716-446655440002_new.png",
                                fileSize = 1024,
                                contentType = "image/png",
                            ),
                    ),
                )

                verify(exactly = 1) { existingFile.markDeleted() }
                verify(exactly = 1) { fileRepository.save(any<File>()) }
                club.profileImageStorageKey shouldBe "CLUB_PROFILE/2026-03/550e8400-e29b-41d4-a716-446655440002_new.png"
            }

            it("이미지 필드가 null이면 File 관련 작업이 실행되지 않는다") {
                val club =
                    ClubTestFixture.createClub(
                        clubContact =
                            ClubContact.from(
                                email = "club@example.com",
                                phoneNumber = "01000000000",
                                primaryContact = PrimaryContact.PHONE,
                            ),
                    )
                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubRepository.getClubById(1L) } returns club

                useCase.update(1L, 10L, ClubUpdateRequest(name = "새 이름"))

                verify(exactly = 0) {
                    fileRepository.findAllByOwnerTypeAndOwnerIdAndStatus(any(), any(), any())
                }
                verify(exactly = 0) { fileRepository.save(any<File>()) }
            }

            it("모든 필드가 null이면 기존 값이 유지된다") {
                val club =
                    ClubTestFixture.createClub(
                        description = "기존 소개",
                        clubContact =
                            ClubContact.from(
                                email = "club@example.com",
                                phoneNumber = "01000000000",
                                primaryContact = PrimaryContact.PHONE,
                            ),
                    )
                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubRepository.getClubById(1L) } returns club

                useCase.update(1L, 10L, ClubUpdateRequest())

                club.name shouldBe "테스트 동아리"
                club.schoolName shouldBe "가천대학교"
                club.description shouldBe "기존 소개"
                club.clubContact.email shouldBe "club@example.com"
                club.clubContact.phoneNumber shouldBe "01000000000"
            }
        }

        describe("deleteProfileImage") {
            it("프로필 사진만 삭제하고 배경 사진은 유지한다") {
                val club =
                    ClubTestFixture.createClub(
                        clubContact =
                            ClubContact.from(
                                email = "club@example.com",
                                phoneNumber = "01011112222",
                                primaryContact = PrimaryContact.PHONE,
                            ),
                    )
                club.update(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "CLUB_PROFILE/2026-02/uuid_profile.png",
                    "CLUB_BACKGROUND/2026-02/uuid_background.png",
                )

                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubRepository.getClubById(1L) } returns club

                useCase.deleteProfileImage(1L, 10L)

                club.profileImageStorageKey shouldBe null
                club.backgroundImageStorageKey shouldBe "CLUB_BACKGROUND/2026-02/uuid_background.png"
            }

            it("기존 File 레코드가 DELETED 처리된다") {
                val existingFile = mockk<File>(relaxed = true)
                val club =
                    ClubTestFixture.createClub(
                        clubContact =
                            ClubContact.from(
                                email = "club@example.com",
                                phoneNumber = "01011112222",
                                primaryContact = PrimaryContact.PHONE,
                            ),
                    )

                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubRepository.getClubById(1L) } returns club
                every {
                    fileRepository.findAllByOwnerTypeAndOwnerIdAndStatus(
                        FileOwnerType.CLUB_PROFILE,
                        1L,
                        FileStatus.UPLOADED,
                    )
                } returns listOf(existingFile)

                useCase.deleteProfileImage(1L, 10L)

                verify(exactly = 1) { existingFile.markDeleted() }
            }
        }

        describe("deleteBackgroundImage") {
            it("배경 사진만 삭제하고 프로필 사진은 유지한다") {
                val club =
                    ClubTestFixture.createClub(
                        clubContact =
                            ClubContact.from(
                                email = "club@example.com",
                                phoneNumber = "01011112222",
                                primaryContact = PrimaryContact.PHONE,
                            ),
                    )
                club.update(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "CLUB_PROFILE/2026-02/uuid_profile.png",
                    "CLUB_BACKGROUND/2026-02/uuid_background.png",
                )

                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubRepository.getClubById(1L) } returns club

                useCase.deleteBackgroundImage(1L, 10L)

                club.profileImageStorageKey shouldBe "CLUB_PROFILE/2026-02/uuid_profile.png"
                club.backgroundImageStorageKey shouldBe null
            }

            it("기존 File 레코드가 DELETED 처리된다") {
                val existingFile = mockk<File>(relaxed = true)
                val club =
                    ClubTestFixture.createClub(
                        clubContact =
                            ClubContact.from(
                                email = "club@example.com",
                                phoneNumber = "01011112222",
                                primaryContact = PrimaryContact.PHONE,
                            ),
                    )

                every { clubPermissionPolicy.requireAdmin(1L, 10L) } returns adminMember
                every { clubRepository.getClubById(1L) } returns club
                every {
                    fileRepository.findAllByOwnerTypeAndOwnerIdAndStatus(
                        FileOwnerType.CLUB_BACKGROUND,
                        1L,
                        FileStatus.UPLOADED,
                    )
                } returns listOf(existingFile)

                useCase.deleteBackgroundImage(1L, 10L)

                verify(exactly = 1) { existingFile.markDeleted() }
            }
        }
    })
