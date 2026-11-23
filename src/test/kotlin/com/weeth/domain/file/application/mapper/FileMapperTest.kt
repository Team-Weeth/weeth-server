package com.weeth.domain.file.application.mapper

import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import io.kotest.core.spec.style.DescribeSpec
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
        }
    })
