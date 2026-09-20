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

            it("문서 타입을 조회한다") {
                FileType.fromExtension("pptx") shouldBe FileType.PPTX
                FileType.fromExtension("docx") shouldBe FileType.DOCX
                FileType.fromContentType(
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                ) shouldBe FileType.PPTX
                FileType.fromContentType(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                ) shouldBe FileType.DOCX
            }

            it("허용 목록 밖의 타입은 null을 반환한다") {
                // svg는 XML이라 스크립트 삽입이 가능해 의도적으로 제외한다
                FileType.fromExtension("svg") shouldBe null
                FileType.fromContentType("image/svg+xml") shouldBe null
                FileType.fromExtension("mp4") shouldBe null
            }
        }
    })
