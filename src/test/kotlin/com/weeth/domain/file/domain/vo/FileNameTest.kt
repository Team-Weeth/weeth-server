package com.weeth.domain.file.domain.vo

import com.weeth.domain.file.application.exception.UnsupportedFileExtensionException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class FileNameTest :
    DescribeSpec({
        describe("FileName") {
            it("파일명 sanitize와 확장자 검증을 수행한다") {
                val fileName = FileName(" report:2026.PDF ")

                fileName.sanitized shouldBe "report_2026.PDF"
                fileName.extension.normalized shouldBe "pdf"
                fileName.extension.fileType shouldBe FileType.PDF
            }

            it("허용되지 않은 확장자는 예외를 던진다") {
                shouldThrow<UnsupportedFileExtensionException> {
                    FileName("payload.exe")
                }
            }
        }
    })
