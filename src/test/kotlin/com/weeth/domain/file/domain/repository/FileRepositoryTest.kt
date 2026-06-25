package com.weeth.domain.file.domain.repository

import com.weeth.config.TestContainersConfig
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.enums.FileStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime
import java.util.UUID

@DataJpaTest
@Import(TestContainersConfig::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FileRepositoryTest(
    private val fileRepository: FileRepository,
    private val jdbcTemplate: JdbcTemplate,
) : DescribeSpec({
        describe("save") {
            it("파일 정보를 저장하고 조회한다") {
                val saved =
                    fileRepository.save(
                        createTestFile(
                            fileName = "notice-image.png",
                            ownerType = FileOwnerType.POST,
                            ownerId = 101L,
                            status = FileStatus.UPLOADED,
                        ),
                    )

                val found = fileRepository.findById(saved.id).orElseThrow()

                found.fileName shouldBe "notice-image.png"
                found.ownerType shouldBe FileOwnerType.POST
                found.ownerId shouldBe 101L
                found.status shouldBe FileStatus.UPLOADED
            }
        }

        describe("findAll/exists") {
            it("ownerType + ownerId + status 조건에 맞고 삭제 예약되지 않은 데이터만 조회한다") {
                fileRepository.save(createTestFile("target-1.png", FileOwnerType.POST, 77L, FileStatus.UPLOADED))
                fileRepository.save(createTestFile("target-2.png", FileOwnerType.POST, 77L, FileStatus.UPLOADED))
                fileRepository.save(createTestFile("deleted.png", FileOwnerType.POST, 77L, FileStatus.DELETED))
                fileRepository.save(
                    createTestFile(
                        fileName = "soft-deleted.png",
                        ownerType = FileOwnerType.POST,
                        ownerId = 77L,
                        status = FileStatus.UPLOADED,
                        isDeleted = true,
                    ),
                )
                fileRepository.save(createTestFile("other-owner.png", FileOwnerType.POST, 78L, FileStatus.UPLOADED))
                fileRepository.save(createTestFile("other-type.png", FileOwnerType.RECEIPT, 77L, FileStatus.UPLOADED))
                fileRepository.save(
                    createTestFile(
                        fileName = "only-soft-deleted.png",
                        ownerType = FileOwnerType.POST,
                        ownerId = 99L,
                        status = FileStatus.UPLOADED,
                        isDeleted = true,
                    ),
                )

                val uploaded = fileRepository.findAll(FileOwnerType.POST, 77L, FileStatus.UPLOADED)
                val allStatus = fileRepository.findAll(FileOwnerType.POST, 77L, null)

                uploaded.map { it.fileName }.sorted() shouldContainExactly listOf("target-1.png", "target-2.png")
                allStatus.map { it.fileName }.sorted() shouldContainExactly
                    listOf("deleted.png", "target-1.png", "target-2.png")

                fileRepository.exists(FileOwnerType.POST, 77L, FileStatus.UPLOADED).shouldBeTrue()
                fileRepository.exists(FileOwnerType.POST, 77L, FileStatus.DELETED).shouldBeTrue()
                fileRepository.exists(FileOwnerType.POST, 99L, FileStatus.UPLOADED).shouldBeFalse()
            }

            it("ownerId 목록 조회에서도 삭제 예약 파일은 제외한다") {
                fileRepository.save(createTestFile("target-1.png", FileOwnerType.POST, 77L, FileStatus.UPLOADED))
                fileRepository.save(createTestFile("target-2.png", FileOwnerType.POST, 78L, FileStatus.UPLOADED))
                fileRepository.save(
                    createTestFile(
                        fileName = "soft-deleted.png",
                        ownerType = FileOwnerType.POST,
                        ownerId = 78L,
                        status = FileStatus.UPLOADED,
                        isDeleted = true,
                    ),
                )

                val files = fileRepository.findAll(FileOwnerType.POST, listOf(77L, 78L), FileStatus.UPLOADED)

                files.map { it.fileName }.sorted() shouldContainExactly listOf("target-1.png", "target-2.png")
            }
        }

        describe("index usage") {
            it("owner_type + owner_id 조건 조회 시 복합 인덱스를 사용한다") {
                fileRepository.save(createTestFile("index-target.png", FileOwnerType.RECEIPT, 55L, FileStatus.UPLOADED))

                val explain =
                    jdbcTemplate
                        .queryForList(
                            "EXPLAIN SELECT id FROM `file` WHERE owner_type = ? AND owner_id = ?",
                            FileOwnerType.RECEIPT.name,
                            55L,
                        ).first()

                val possibleKeys = explain.valueBy("possible_keys")
                val selectedKey = explain.valueBy("key")

                possibleKeys shouldContain "idx_file_owner_type_owner_id"
                selectedKey shouldBe "idx_file_owner_type_owner_id"
            }
        }
    })

private fun createTestFile(
    fileName: String,
    ownerType: FileOwnerType,
    ownerId: Long,
    status: FileStatus,
    isDeleted: Boolean = false,
): File =
    File
        .createUploaded(
            fileName = fileName,
            storageKey = "${ownerType.name}/2026-02/${UUID.randomUUID()}_$fileName",
            fileSize = 1024L,
            contentType = "image/png",
            ownerType = ownerType,
            ownerId = ownerId,
        ).also {
            if (status == FileStatus.DELETED) {
                ReflectionTestUtils.setField(it, "status", FileStatus.DELETED)
            }
            if (isDeleted) {
                it.markDeleted(LocalDateTime.of(2026, 6, 25, 12, 0))
            }
        }

private fun Map<String, Any?>.valueBy(key: String): String =
    entries
        .first {
            it.key.equals(key, ignoreCase = true)
        }.value
        .toString()
