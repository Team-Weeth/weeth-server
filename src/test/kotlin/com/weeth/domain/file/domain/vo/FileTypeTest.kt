package com.weeth.domain.file.domain.vo

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class FileTypeTest :
    DescribeSpec({
        describe("FileType") {
            it("contentType으로 타입을 조회한다") {
                FileType.fromContentType("image/png") shouldBe FileType.PNG
                FileType.fromContentType("application/pdf") shouldBe FileType.PDF
            }

            it("extension으로 타입을 조회한다") {
                FileType.fromExtension("jpg") shouldBe FileType.JPEG
                FileType.fromExtension("jpeg") shouldBe FileType.JPEG
                FileType.fromExtension("webp") shouldBe FileType.WEBP
            }
        }
    })
