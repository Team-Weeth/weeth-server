package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.file.application.dto.request.FileSaveRequest
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.user.application.dto.request.CreateMultiProfileRequest
import com.weeth.domain.user.application.dto.request.UpdateMultiProfileRequest
import com.weeth.domain.user.application.exception.UserProfileNotFoundException
import com.weeth.domain.user.application.mapper.UserProfileMapper
import com.weeth.domain.user.domain.entity.UserProfile
import com.weeth.domain.user.domain.repository.UserProfileRepository
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.test.util.ReflectionTestUtils
import java.util.Optional

class ManageUserProfileUseCaseTest :
    DescribeSpec({
        val userRepository = mockk<UserRepository>()
        val userProfileRepository = mockk<UserProfileRepository>()
        val fileRepository = mockk<FileRepository>()
        val fileAccessUrlPort = mockk<FileAccessUrlPort>()
        val userProfileMapper = UserProfileMapper(fileAccessUrlPort)
        val useCase =
            ManageUserProfileUseCase(
                userRepository = userRepository,
                userProfileRepository = userProfileRepository,
                fileRepository = fileRepository,
                userProfileMapper = userProfileMapper,
            )

        beforeTest {
            clearMocks(userRepository, userProfileRepository, fileRepository, fileAccessUrlPort)
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
        }

        describe("update") {
            it("로그인 사용자의 프로필 정보를 부분 수정하고 이미지 파일을 교체하거나 삭제한다") {
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
                        removeHeaderImage = true,
                        bio = "  새 소개  ",
                    )
                every { userProfileRepository.findByIdAndUserId(10L, 1L) } returns Optional.of(profile)
                every {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(FileOwnerType.USER_PROFILE_IMAGE, 10L)
                } returns 1
                every {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(FileOwnerType.USER_PROFILE_HEADER, 10L)
                } returns 1
                every { fileRepository.save(any<File>()) } answers { firstArg() }
                every { fileAccessUrlPort.resolve(newProfileStorageKey) } returns "https://cdn.test/new.png"

                val result = useCase.update(1L, 10L, request)

                result.profileId shouldBe 10L
                result.name shouldBe "새 이름"
                result.profileImageUrl shouldBe "https://cdn.test/new.png"
                result.headerImageUrl shouldBe null
                result.bio shouldBe "새 소개"
                profile.profileImageStorageKey shouldBe newProfileStorageKey
                profile.headerImageStorageKey shouldBe null
                verify(exactly = 1) {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(FileOwnerType.USER_PROFILE_IMAGE, 10L)
                }
                verify(exactly = 1) {
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(FileOwnerType.USER_PROFILE_HEADER, 10L)
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
                every { userProfileRepository.findByIdAndUserId(10L, 1L) } returns Optional.empty()

                shouldThrow<UserProfileNotFoundException> {
                    useCase.update(1L, 10L, request)
                }

                verify(exactly = 0) { fileRepository.save(any<File>()) }
                verify(exactly = 0) { fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(any(), any()) }
            }
        }
    })
