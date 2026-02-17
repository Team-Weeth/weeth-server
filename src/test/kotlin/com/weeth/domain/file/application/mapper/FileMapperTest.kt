package com.weeth.domain.file.application.mapper

import com.weeth.domain.file.application.dto.request.FileSaveRequest
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.mockk

class FileMapperTest :
    DescribeSpec({
        val fileAccessUrlPort = mockk<FileAccessUrlPort>(relaxed = true)
        val fileMapper = FileMapper(fileAccessUrlPort)

        describe("toFileList") {
            it("요청이 null이면 빈 리스트를 반환한다") {
                val result = fileMapper.toFileList(null, FileOwnerType.POST, 1L)

                result shouldBe emptyList()
            }

            it("요청이 비어있으면 빈 리스트를 반환한다") {
                val result = fileMapper.toFileList(emptyList(), FileOwnerType.POST, 1L)

                result shouldBe emptyList()
            }

            it("요청 리스트를 ownerType/ownerId를 포함한 File 리스트로 매핑한다") {
                val requests =
                    listOf(
                        FileSaveRequest("a.png", "POST/2026-02/a.png", 100L, "image/png"),
                        FileSaveRequest("b.pdf", "POST/2026-02/b.pdf", 200L, "application/pdf"),
                    )

                val result = fileMapper.toFileList(requests, FileOwnerType.POST, 99L)

                result shouldHaveSize 2
                result[0].fileName shouldBe "a.png"
                result[0].ownerType shouldBe FileOwnerType.POST
                result[0].ownerId shouldBe 99L
                result[1].fileName shouldBe "b.pdf"
                result[1].ownerType shouldBe FileOwnerType.POST
                result[1].ownerId shouldBe 99L
            }
        }
    })
