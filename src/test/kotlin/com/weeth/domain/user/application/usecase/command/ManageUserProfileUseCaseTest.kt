package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.club.domain.repository.ClubMemberRepository
import com.weeth.domain.club.fixture.ClubMemberTestFixture
import com.weeth.domain.club.fixture.ClubTestFixture
import com.weeth.domain.file.application.dto.request.FileSaveRequest
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.user.application.dto.request.AssignClubProfileRequest
import com.weeth.domain.user.application.dto.request.ClubProfileAssignmentRequest
import com.weeth.domain.user.application.dto.request.CreateMultiProfileRequest
import com.weeth.domain.user.application.dto.request.UpdateMultiProfileRequest
import com.weeth.domain.user.application.exception.UserProfileAssignmentNotAllowedException
import com.weeth.domain.user.application.exception.UserProfileDuplicateClubAssignmentException
import com.weeth.domain.user.application.exception.UserProfileInUseException
import com.weeth.domain.user.application.exception.UserProfileInvalidClubIdException
import com.weeth.domain.user.application.exception.UserProfileNotFoundException
import com.weeth.domain.user.application.mapper.UserProfileMapper
import com.weeth.domain.user.domain.entity.UserProfile
import com.weeth.domain.user.domain.repository.UserProfileRepository
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.domain.user.fixture.UserTestFixture
import com.weeth.global.common.id.TsidBase62Encoder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.util.ReflectionTestUtils
import java.util.Optional

class ManageUserProfileUseCaseTest :
    DescribeSpec({
        val userRepository = mockk<UserRepository>()
        val userProfileRepository = mockk<UserProfileRepository>()
        val clubMemberRepository = mockk<ClubMemberRepository>()
        val fileRepository = mockk<FileRepository>()
        val fileAccessUrlPort = mockk<FileAccessUrlPort>()
        val userProfileMapper = UserProfileMapper(fileAccessUrlPort)
        val useCase =
            ManageUserProfileUseCase(
                userRepository = userRepository,
                userProfileRepository = userProfileRepository,
                clubMemberRepository = clubMemberRepository,
                fileRepository = fileRepository,
                userProfileMapper = userProfileMapper,
            )

        beforeTest {
            clearMocks(userRepository, userProfileRepository, clubMemberRepository, fileRepository, fileAccessUrlPort)
        }

        describe("create") {
            it("사용자 프로필을 생성하고 이미지 파일 메타데이터를 저장한다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                val profileStorageKey = "USER_PROFILE_IMAGE/2026-07/123e4567-e89b-12d3-a456-426614174000_profile.png"
                val headerStorageKey = "USER_PROFILE_HEADER/2026-07/123e4567-e89b-12d3-a456-426614174001_header.png"
                val request =
                    CreateMultiProfileRequest(
                        name = "  길동  ",
                        profileImage =
                            FileSaveRequest(
                                fileName = "profile.png",
                                storageKey = profileStorageKey,
                                fileSize = 100L,
                                contentType = "image/png",
                            ),
                        headerImage =
                            FileSaveRequest(
                                fileName = "header.png",
                                storageKey = headerStorageKey,
                                fileSize = 200L,
                                contentType = "image/png",
                            ),
                        bio = "  안녕하세요  ",
                    )
                every { userRepository.getById(1L) } returns user
                every { userProfileRepository.save(any<UserProfile>()) } answers {
                    firstArg<UserProfile>().apply {
                        ReflectionTestUtils.setField(this, "id", 10L)
                    }
                }
                every { fileRepository.save(any<File>()) } answers { firstArg() }
                every { fileAccessUrlPort.resolve(profileStorageKey) } returns "https://cdn.test/profile.png"
                every { fileAccessUrlPort.resolve(headerStorageKey) } returns "https://cdn.test/header.png"

                val result = useCase.create(userId = 1L, request = request)

                result.profileId shouldBe 10L
                result.name shouldBe "길동"
                result.profileImageUrl shouldBe "https://cdn.test/profile.png"
                result.headerImageUrl shouldBe "https://cdn.test/header.png"
                result.bio shouldBe "안녕하세요"
                verify(exactly = 1) {
                    fileRepository.save(
                        match {
                            it.ownerType == FileOwnerType.USER_PROFILE_IMAGE &&
                                it.ownerId == 10L &&
                                it.storageKey.value == profileStorageKey
                        },
                    )
                }
                verify(exactly = 1) {
                    fileRepository.save(
                        match {
                            it.ownerType == FileOwnerType.USER_PROFILE_HEADER &&
                                it.ownerId == 10L &&
                                it.storageKey.value == headerStorageKey
                        },
                    )
                }
            }

            it("이미지 요청이 없으면 파일 메타데이터를 저장하지 않는다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                val request = CreateMultiProfileRequest(name = "길동")
                every { userRepository.getById(1L) } returns user
                every { userProfileRepository.save(any<UserProfile>()) } answers {
                    firstArg<UserProfile>().apply {
                        ReflectionTestUtils.setField(this, "id", 10L)
                    }
                }

                val result = useCase.create(userId = 1L, request = request)

                result.profileId shouldBe 10L
                result.name shouldBe "길동"
                result.profileImageUrl shouldBe null
                result.headerImageUrl shouldBe null
                verify(exactly = 0) { fileRepository.save(any<File>()) }
            }

            it("생성 요청에 clubIds가 있으면 생성한 프로필을 해당 ACTIVE 동아리에 바로 할당한다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                val club1 = ClubTestFixture.createClub(id = 100L, name = "동아리1")
                val club2 = ClubTestFixture.createClub(id = 101L, name = "동아리2")
                val member1 = ClubMemberTestFixture.createActiveMember(id = 1000L, club = club1, user = user)
                val member2 = ClubMemberTestFixture.createActiveMember(id = 1001L, club = club2, user = user)
                val request =
                    CreateMultiProfileRequest(
                        name = "길동",
                        clubIds = listOf(TsidBase62Encoder.encode(100L), TsidBase62Encoder.encode(101L)),
                    )
                every { userRepository.getById(1L) } returns user
                every { userProfileRepository.save(any<UserProfile>()) } answers {
                    firstArg<UserProfile>().apply {
                        ReflectionTestUtils.setField(this, "id", 10L)
                    }
                }
                every {
                    clubMemberRepository.findAllActiveByUserIdAndClubIdsWithLock(1L, listOf(100L, 101L))
                } returns listOf(member1, member2)

                val result = useCase.create(userId = 1L, request = request)

                result.profileId shouldBe 10L
                member1.userProfile?.id shouldBe 10L
                member2.userProfile?.id shouldBe 10L
            }
        }

        describe("update") {
            it("로그인 사용자의 프로필 정보를 부분 수정하고 이미지 파일을 교체한다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                val oldProfileStorageKey = "USER_PROFILE_IMAGE/2026-07/123e4567-e89b-12d3-a456-426614174000_old.png"
                val oldHeaderStorageKey = "USER_PROFILE_HEADER/2026-07/123e4567-e89b-12d3-a456-426614174001_old.png"
                val profile =
                    UserProfile
                        .create(
                            user = user,
                            name = "기존 이름",
                            profileImageStorageKey = oldProfileStorageKey,
                            headerImageStorageKey = oldHeaderStorageKey,
                            bio = "기존 소개",
                        ).apply {
                            ReflectionTestUtils.setField(this, "id", 10L)
                        }
                val newProfileStorageKey = "USER_PROFILE_IMAGE/2026-07/123e4567-e89b-12d3-a456-426614174002_new.png"
                val request =
                    UpdateMultiProfileRequest(
                        name = "  새 이름  ",
                        profileImage =
                            FileSaveRequest(
                                fileName = "new.png",
                                storageKey = newProfileStorageKey,
                                fileSize = 100L,
                                contentType = "image/png",
                            ),
                        bio = "  새 소개  ",
                    )
                every { userProfileRepository.findByIdAndUserIdWithLock(10L, 1L) } returns Optional.of(profile)
                every {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(FileOwnerType.USER_PROFILE_IMAGE, 10L)
                } returns 1
                every { fileRepository.save(any<File>()) } answers { firstArg() }
                every { fileAccessUrlPort.resolve(newProfileStorageKey) } returns "https://cdn.test/new.png"
                every { fileAccessUrlPort.resolve(oldHeaderStorageKey) } returns "https://cdn.test/header.png"

                val result = useCase.update(1L, 10L, request)

                result.profileId shouldBe 10L
                result.name shouldBe "새 이름"
                result.profileImageUrl shouldBe "https://cdn.test/new.png"
                result.headerImageUrl shouldBe "https://cdn.test/header.png"
                result.bio shouldBe "새 소개"
                profile.profileImageStorageKey shouldBe newProfileStorageKey
                profile.headerImageStorageKey shouldBe oldHeaderStorageKey
                verify(exactly = 1) {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(FileOwnerType.USER_PROFILE_IMAGE, 10L)
                }
                verify(exactly = 1) {
                    fileRepository.save(
                        match {
                            it.ownerType == FileOwnerType.USER_PROFILE_IMAGE &&
                                it.ownerId == 10L &&
                                it.storageKey.value == newProfileStorageKey
                        },
                    )
                }
            }

            it("프로필이 없거나 로그인 사용자 소유가 아니면 예외가 발생한다") {
                val request = UpdateMultiProfileRequest(name = "새 이름")
                every { userProfileRepository.findByIdAndUserIdWithLock(10L, 1L) } returns Optional.empty()

                shouldThrow<UserProfileNotFoundException> {
                    useCase.update(1L, 10L, request)
                }

                verify(exactly = 0) { fileRepository.save(any<File>()) }
                verify(exactly = 0) { fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(any(), any()) }
            }
        }

        describe("deleteProfileImage") {
            it("프로필 사진만 삭제하고 헤더 사진은 유지한다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                val profile =
                    UserProfile
                        .create(
                            user = user,
                            name = "프로필",
                            profileImageStorageKey = "USER_PROFILE_IMAGE/2026-07/profile.png",
                            headerImageStorageKey = "USER_PROFILE_HEADER/2026-07/header.png",
                        ).apply {
                            ReflectionTestUtils.setField(this, "id", 10L)
                        }
                every { userProfileRepository.findByIdAndUserIdWithLock(10L, 1L) } returns Optional.of(profile)
                every {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(FileOwnerType.USER_PROFILE_IMAGE, 10L)
                } returns 1

                useCase.deleteProfileImage(userId = 1L, profileId = 10L)

                profile.profileImageStorageKey shouldBe null
                profile.headerImageStorageKey shouldBe "USER_PROFILE_HEADER/2026-07/header.png"
                verify(exactly = 1) {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(FileOwnerType.USER_PROFILE_IMAGE, 10L)
                }
            }
        }

        describe("deleteHeaderImage") {
            it("헤더 사진만 삭제하고 프로필 사진은 유지한다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                val profile =
                    UserProfile
                        .create(
                            user = user,
                            name = "프로필",
                            profileImageStorageKey = "USER_PROFILE_IMAGE/2026-07/profile.png",
                            headerImageStorageKey = "USER_PROFILE_HEADER/2026-07/header.png",
                        ).apply {
                            ReflectionTestUtils.setField(this, "id", 10L)
                        }
                every { userProfileRepository.findByIdAndUserIdWithLock(10L, 1L) } returns Optional.of(profile)
                every {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(FileOwnerType.USER_PROFILE_HEADER, 10L)
                } returns 1

                useCase.deleteHeaderImage(userId = 1L, profileId = 10L)

                profile.profileImageStorageKey shouldBe "USER_PROFILE_IMAGE/2026-07/profile.png"
                profile.headerImageStorageKey shouldBe null
                verify(exactly = 1) {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(FileOwnerType.USER_PROFILE_HEADER, 10L)
                }
            }
        }

        describe("assignClubProfiles") {
            it("로그인 사용자의 ACTIVE 동아리 멤버십에 본인 프로필을 할당한다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                val club1 = ClubTestFixture.createClub(id = 100L, name = "동아리1")
                val club2 = ClubTestFixture.createClub(id = 101L, name = "동아리2")
                val member1 = ClubMemberTestFixture.createActiveMember(id = 1000L, club = club1, user = user)
                val member2 = ClubMemberTestFixture.createActiveMember(id = 1001L, club = club2, user = user)
                val profile1 =
                    UserProfile.create(user = user, name = "프로필1").apply {
                        ReflectionTestUtils.setField(this, "id", 10L)
                    }
                val profile2 =
                    UserProfile.create(user = user, name = "프로필2").apply {
                        ReflectionTestUtils.setField(this, "id", 11L)
                    }
                val request =
                    AssignClubProfileRequest(
                        assignments =
                            listOf(
                                ClubProfileAssignmentRequest(TsidBase62Encoder.encode(100L), 10L),
                                ClubProfileAssignmentRequest(TsidBase62Encoder.encode(101L), 11L),
                            ),
                    )
                every { userProfileRepository.findAllByUserIdAndIdInWithLock(1L, listOf(10L, 11L)) } returns
                    listOf(profile1, profile2)
                every {
                    clubMemberRepository.findAllActiveByUserIdAndClubIdsWithLock(1L, listOf(100L, 101L))
                } returns listOf(member1, member2)

                useCase.assignClubProfiles(1L, request)

                member1.userProfile shouldBe profile1
                member2.userProfile shouldBe profile2
            }

            it("같은 동아리의 프로필 설정이 중복되면 예외가 발생한다") {
                val clubId = TsidBase62Encoder.encode(100L)
                val request =
                    AssignClubProfileRequest(
                        assignments =
                            listOf(
                                ClubProfileAssignmentRequest(clubId, 10L),
                                ClubProfileAssignmentRequest(clubId, 11L),
                            ),
                    )

                shouldThrow<UserProfileDuplicateClubAssignmentException> {
                    useCase.assignClubProfiles(1L, request)
                }
            }

            it("clubId가 Base62 형식이 아니면 예외가 발생한다") {
                val request =
                    AssignClubProfileRequest(
                        assignments =
                            listOf(
                                ClubProfileAssignmentRequest("%%%", 10L),
                            ),
                    )

                shouldThrow<UserProfileInvalidClubIdException> {
                    useCase.assignClubProfiles(1L, request)
                }
            }

            it("본인 소유가 아닌 프로필이 포함되면 예외가 발생한다") {
                val request =
                    AssignClubProfileRequest(
                        assignments =
                            listOf(
                                ClubProfileAssignmentRequest(TsidBase62Encoder.encode(100L), 10L),
                                ClubProfileAssignmentRequest(TsidBase62Encoder.encode(101L), 11L),
                            ),
                    )
                every { userProfileRepository.findAllByUserIdAndIdInWithLock(1L, listOf(10L, 11L)) } returns emptyList()

                shouldThrow<UserProfileNotFoundException> {
                    useCase.assignClubProfiles(1L, request)
                }
            }

            it("ACTIVE 멤버십이 아닌 동아리가 포함되면 예외가 발생한다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                val profile =
                    UserProfile.create(user = user, name = "프로필").apply {
                        ReflectionTestUtils.setField(this, "id", 10L)
                    }
                val request =
                    AssignClubProfileRequest(
                        assignments =
                            listOf(
                                ClubProfileAssignmentRequest(TsidBase62Encoder.encode(100L), 10L),
                            ),
                    )
                every { userProfileRepository.findAllByUserIdAndIdInWithLock(1L, listOf(10L)) } returns listOf(profile)
                every {
                    clubMemberRepository.findAllActiveByUserIdAndClubIdsWithLock(1L, listOf(100L))
                } returns emptyList()

                shouldThrow<UserProfileAssignmentNotAllowedException> {
                    useCase.assignClubProfiles(1L, request)
                }
            }
        }

        describe("delete") {
            it("로그인 사용자의 미사용 프로필을 삭제하고 이미지 파일 메타데이터를 삭제한다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                val profile =
                    UserProfile
                        .create(
                            user = user,
                            name = "삭제할 프로필",
                            profileImageStorageKey = "USER_PROFILE_IMAGE/2026-07/profile.png",
                            headerImageStorageKey = "USER_PROFILE_HEADER/2026-07/header.png",
                        ).apply {
                            ReflectionTestUtils.setField(this, "id", 10L)
                        }
                every { userProfileRepository.findByIdAndUserIdWithLock(10L, 1L) } returns Optional.of(profile)
                every { clubMemberRepository.existsByUserProfileIdAndMemberStatus(any(), any()) } returns false
                every {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(FileOwnerType.USER_PROFILE_IMAGE, 10L)
                } returns 1
                every {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(FileOwnerType.USER_PROFILE_HEADER, 10L)
                } returns 1
                every { clubMemberRepository.clearUserProfileReferences(10L) } returns 1
                justRun { userProfileRepository.delete(profile) }

                useCase.delete(1L, 10L)

                verify(exactly = 1) {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(FileOwnerType.USER_PROFILE_IMAGE, 10L)
                }
                verify(exactly = 1) {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(FileOwnerType.USER_PROFILE_HEADER, 10L)
                }
                verify(exactly = 1) { clubMemberRepository.clearUserProfileReferences(10L) }
                verify(exactly = 1) { userProfileRepository.delete(profile) }
            }

            it("사용 중인 프로필이면 예외가 발생한다") {
                val user = UserTestFixture.createRegisteredUser(1L)
                val profile =
                    UserProfile.create(user = user, name = "사용 중인 프로필").apply {
                        ReflectionTestUtils.setField(this, "id", 10L)
                    }
                every { userProfileRepository.findByIdAndUserIdWithLock(10L, 1L) } returns Optional.of(profile)
                every { clubMemberRepository.existsByUserProfileIdAndMemberStatus(10L, any()) } returns true

                shouldThrow<UserProfileInUseException> {
                    useCase.delete(1L, 10L)
                }

                verify(exactly = 0) { userProfileRepository.delete(any()) }
                verify(exactly = 0) { fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(any(), any()) }
            }
        }
    })
