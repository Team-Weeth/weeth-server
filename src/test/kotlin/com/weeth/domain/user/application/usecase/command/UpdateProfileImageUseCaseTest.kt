package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.file.application.dto.request.FileSaveRequest
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.enums.FileStatus
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.file.domain.repository.FileRepository
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.domain.user.fixture.UserTestFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class UpdateProfileImageUseCaseTest :
    DescribeSpec({
        val userRepository = mockk<UserRepository>()
        val fileRepository = mockk<FileRepository>()
        val fileAccessUrlPort = mockk<FileAccessUrlPort>()
        val useCase = UpdateProfileImageUseCase(userRepository, fileRepository, fileAccessUrlPort)

        beforeTest { clearMocks(userRepository, fileRepository, fileAccessUrlPort) }

        describe("execute") {
            val request =
                FileSaveRequest(
                    fileName = "profileImage.png",
                    storageKey = "USER_PROFILE/2026-03/550e8400-e29b-41d4-a716-446655440000_profileImage.png",
                    fileSize = 102400,
                    contentType = "image/png",
                )

            context("기존 프로필 이미지가 없는 경우") {
                it("새 파일을 저장하고 profileImageUrl을 업데이트한다") {
                    val user = UserTestFixture.createActiveUser1(1L)
                    every { userRepository.getById(1L) } returns user
                    every {
                        fileRepository.findAllByOwnerTypeAndOwnerIdAndStatus(
                            FileOwnerType.USER_PROFILE,
                            1L,
                            FileStatus.UPLOADED,
                        )
                    } returns emptyList()
                    every { fileRepository.save(any<File>()) } answers { firstArg() }
                    every { fileAccessUrlPort.resolve(any()) } returns "https://cdn.test.com/profileImage.png"

                    useCase.execute(1L, request)

                    user.profileImageUrl shouldBe "https://cdn.test.com/profileImage.png"
                    verify(exactly = 1) { fileRepository.save(any<File>()) }
                }
            }

            context("기존 프로필 이미지가 있는 경우") {
                it("기존 파일을 soft delete하고 새 파일을 저장한다") {
                    val user = UserTestFixture.createActiveUser1(1L)
                    val existingFile = mockk<File>(relaxed = true)
                    every { userRepository.getById(1L) } returns user
                    every {
                        fileRepository.findAllByOwnerTypeAndOwnerIdAndStatus(
                            FileOwnerType.USER_PROFILE,
                            1L,
                            FileStatus.UPLOADED,
                        )
                    } returns listOf(existingFile)
                    every { fileRepository.save(any<File>()) } answers { firstArg() }
                    every { fileAccessUrlPort.resolve(any()) } returns "https://cdn.test.com/new-profileImage.png"

                    useCase.execute(1L, request)

                    verify(exactly = 1) { existingFile.markDeleted() }
                    verify(exactly = 1) { fileRepository.save(any<File>()) }
                    user.profileImageUrl shouldContain "new-profileImage.png"
                }
            }
        }
    })
