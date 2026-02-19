package com.weeth.domain.file.domain.entity

import com.weeth.domain.file.application.exception.UnsupportedFileContentTypeException
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.entity.FileStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class FileTest :
    DescribeSpec({
        describe("createUploaded") {
            it("유효한 입력이면 업로드 상태 파일을 생성한다") {
                val file =
                    File.createUploaded(
                        fileName = "image.png",
                        storageKey = "POST/2026-02/550e8400-e29b-41d4-a716-446655440000_image.png",
                        fileSize = 1024,
                        contentType = "image/png",
                        ownerType = FileOwnerType.POST,
                        ownerId = 1L,
                    )

                file.fileName shouldBe "image.png"
                file.ownerType shouldBe FileOwnerType.POST
                file.status shouldBe FileStatus.UPLOADED
            }

            it("fileName이 blank면 예외가 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    File.createUploaded(
                        fileName = " ",
                        storageKey = "POST/2026-02/550e8400-e29b-41d4-a716-446655440000_image.png",
                        fileSize = 1024,
                        contentType = "image/png",
                        ownerType = FileOwnerType.POST,
                        ownerId = 1L,
                    )
                }
            }

            it("storageKey가 blank면 예외가 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    File.createUploaded(
                        fileName = "image.png",
                        storageKey = " ",
                        fileSize = 1024,
                        contentType = "image/png",
                        ownerType = FileOwnerType.POST,
                        ownerId = 1L,
                    )
                }
            }

            it("storageKey ownerType 형식이 잘못되면 예외가 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    File.createUploaded(
                        fileName = "image.png",
                        storageKey = "INVALID/2026-02/550e8400-e29b-41d4-a716-446655440000_image.png",
                        fileSize = 1024,
                        contentType = "image/png",
                        ownerType = FileOwnerType.POST,
                        ownerId = 1L,
                    )
                }
            }

            it("storageKey uuid 형식이 잘못되면 예외가 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    File.createUploaded(
                        fileName = "image.png",
                        storageKey = "POST/2026-02/not-uuid_image.png",
                        fileSize = 1024,
                        contentType = "image/png",
                        ownerType = FileOwnerType.POST,
                        ownerId = 1L,
                    )
                }
            }

            it("fileSize가 0 이하이면 예외가 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    File.createUploaded(
                        fileName = "image.png",
                        storageKey = "POST/2026-02/550e8400-e29b-41d4-a716-446655440000_image.png",
                        fileSize = 0,
                        contentType = "image/png",
                        ownerType = FileOwnerType.POST,
                        ownerId = 1L,
                    )
                }
            }

            it("ownerId가 0 이하이면 예외가 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    File.createUploaded(
                        fileName = "image.png",
                        storageKey = "POST/2026-02/550e8400-e29b-41d4-a716-446655440000_image.png",
                        fileSize = 1024,
                        contentType = "image/png",
                        ownerType = FileOwnerType.POST,
                        ownerId = 0,
                    )
                }
            }

            it("허용되지 않은 contentType이면 예외가 발생한다") {
                shouldThrow<UnsupportedFileContentTypeException> {
                    File.createUploaded(
                        fileName = "file.exe",
                        storageKey = "POST/2026-02/550e8400-e29b-41d4-a716-446655440000_file.exe",
                        fileSize = 100,
                        contentType = "application/octet-stream",
                        ownerType = FileOwnerType.POST,
                        ownerId = 1L,
                    )
                }
            }
        }

        describe("markDeleted") {
            it("파일 상태를 DELETED로 변경한다") {
                val file =
                    File.createUploaded(
                        fileName = "doc.pdf",
                        storageKey = "POST/2026-02/550e8400-e29b-41d4-a716-446655440000_doc.pdf",
                        fileSize = 100,
                        contentType = "application/pdf",
                        ownerType = FileOwnerType.POST,
                        ownerId = 2L,
                    )

                file.markDeleted()

                file.status shouldBe FileStatus.DELETED
            }
        }
    })
