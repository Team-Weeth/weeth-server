package com.weeth.domain.file.application.usecase.query

import com.weeth.domain.file.application.dto.response.UrlResponse
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.port.FileUploadUrl
import com.weeth.domain.file.domain.port.FileUploadUrlPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class FileQueryServiceTest :
    DescribeSpec({
        val preSignedService = mockk<FileUploadUrlPort>()
        val fileMapper = mockk<FileMapper>()
        val useCase = FileQueryService(preSignedService, fileMapper)

        beforeEach {
            clearMocks(preSignedService, fileMapper)
        }

        describe("getUrl") {
            it("요청한 파일명 순서대로 presigned URL을 반환한다") {
                val fileNames = listOf("a.png", "b.pdf")
                val ownerType = FileOwnerType.POST
                val responses =
                    listOf(
                        UrlResponse("a.png", "https://presigned/a", "POST/2026-02/a.png"),
                        UrlResponse("b.pdf", "https://presigned/b", "POST/2026-02/b.pdf"),
                    )
                val firstPresigned = FileUploadUrl("a.png", "POST/2026-02/a.png", "https://presigned/a")
                val secondPresigned = FileUploadUrl("b.pdf", "POST/2026-02/b.pdf", "https://presigned/b")

                every { preSignedService.generateUploadUrl(ownerType, "a.png") } returns firstPresigned
                every { preSignedService.generateUploadUrl(ownerType, "b.pdf") } returns secondPresigned
                every { fileMapper.toUrlResponse("a.png", "https://presigned/a", "POST/2026-02/a.png") } returns responses[0]
                every { fileMapper.toUrlResponse("b.pdf", "https://presigned/b", "POST/2026-02/b.pdf") } returns responses[1]

                val result = useCase.generateFileUploadUrls(ownerType, fileNames)

                result shouldContainExactly responses
                verify(exactly = 1) { preSignedService.generateUploadUrl(ownerType, "a.png") }
                verify(exactly = 1) { preSignedService.generateUploadUrl(ownerType, "b.pdf") }
            }

            it("Presigned URL 생성 포트에서 예외가 나면 그대로 전파한다") {
                val ownerType = FileOwnerType.POST
                val fileNames = listOf("a.png")

                every { preSignedService.generateUploadUrl(ownerType, "a.png") } throws RuntimeException("s3 error")

                shouldThrow<RuntimeException> {
                    useCase.generateFileUploadUrls(ownerType, fileNames)
                }

                verify(exactly = 0) { fileMapper.toUrlResponse(any(), any(), any()) }
            }
        }
    })
