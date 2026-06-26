package com.weeth.domain.file.domain.repository

import com.weeth.config.TestContainersConfig
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.enums.FileStatus
import io.kotest.assertions.throwables.shouldThrow
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
        fun row(fileId: Long): Map<String, Any?> =
            jdbcTemplate.queryForMap(
                "SELECT is_deleted, deleted_at, hard_delete_after, modified_at FROM `file` WHERE id = ?",
                fileId,
            )

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
            it("기본 단건 owner 조회는 업로드 상태이고 삭제 예약되지 않은 파일만 반환한다") {
                fileRepository.save(createTestFile("target.png", FileOwnerType.COMMENT, 77L, FileStatus.UPLOADED))
                fileRepository.save(
                    createTestFile("status-deleted.png", FileOwnerType.COMMENT, 77L, FileStatus.DELETED),
                )
                fileRepository.save(
                    createTestFile(
                        fileName = "soft-deleted.png",
                        ownerType = FileOwnerType.COMMENT,
                        ownerId = 77L,
                        status = FileStatus.UPLOADED,
                        isDeleted = true,
                    ),
                )

                val files = fileRepository.findAll(FileOwnerType.COMMENT, 77L)

                files.map { it.fileName } shouldContainExactly listOf("target.png")
            }

            it("기본 ownerId 목록 조회는 업로드 상태이고 삭제 예약되지 않은 파일만 반환한다") {
                fileRepository.save(createTestFile("target-1.png", FileOwnerType.POST, 77L, FileStatus.UPLOADED))
                fileRepository.save(createTestFile("target-2.png", FileOwnerType.POST, 78L, FileStatus.UPLOADED))
                fileRepository.save(createTestFile("status-deleted.png", FileOwnerType.POST, 78L, FileStatus.DELETED))
                fileRepository.save(
                    createTestFile(
                        fileName = "soft-deleted.png",
                        ownerType = FileOwnerType.POST,
                        ownerId = 78L,
                        status = FileStatus.UPLOADED,
                        isDeleted = true,
                    ),
                )

                val files = fileRepository.findAll(FileOwnerType.POST, listOf(77L, 78L))

                files.map { it.fileName }.sorted() shouldContainExactly listOf("target-1.png", "target-2.png")
            }

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

        describe("bulk mark deleted") {
            it("단건 owner의 활성 파일만 삭제 예약한다") {
                val deletedAt = LocalDateTime.of(2026, 6, 25, 12, 0)
                val hardDeleteAfter = deletedAt
                val target =
                    fileRepository.save(createTestFile("target.png", FileOwnerType.POST, 77L, FileStatus.UPLOADED))
                val statusDeleted =
                    fileRepository.save(
                        createTestFile("status-deleted.png", FileOwnerType.POST, 77L, FileStatus.DELETED),
                    )
                val alreadyDeleted =
                    fileRepository.save(
                        createTestFile(
                            fileName = "already-deleted.png",
                            ownerType = FileOwnerType.POST,
                            ownerId = 77L,
                            status = FileStatus.UPLOADED,
                            isDeleted = true,
                        ),
                    )
                val otherOwner =
                    fileRepository.save(createTestFile("other-owner.png", FileOwnerType.POST, 78L, FileStatus.UPLOADED))

                val updatedCount =
                    fileRepository.markActiveDeletedByOwnerTypeAndOwnerId(
                        ownerType = FileOwnerType.POST,
                        ownerId = 77L,
                        deletedAt = deletedAt,
                        hardDeleteAfter = hardDeleteAfter,
                    )

                updatedCount shouldBe 1
                row(target.id).booleanBy("is_deleted").shouldBeTrue()
                row(target.id).localDateTimeBy("deleted_at") shouldBe deletedAt
                row(target.id).localDateTimeBy("hard_delete_after") shouldBe hardDeleteAfter
                row(target.id).localDateTimeBy("modified_at") shouldBe deletedAt
                row(statusDeleted.id).booleanBy("is_deleted").shouldBeFalse()
                row(alreadyDeleted.id).localDateTimeBy("deleted_at") shouldBe LocalDateTime.of(2026, 6, 25, 12, 0)
                row(otherOwner.id).booleanBy("is_deleted").shouldBeFalse()
            }

            it("단건 bulk 삭제 후 같은 트랜잭션의 재조회에서도 갱신 상태를 반환한다") {
                val deletedAt = LocalDateTime.of(2026, 6, 25, 12, 0)
                val target =
                    fileRepository.save(createTestFile("target.png", FileOwnerType.POST, 77L, FileStatus.UPLOADED))

                fileRepository.markActiveDeletedByOwnerTypeAndOwnerId(
                    ownerType = FileOwnerType.POST,
                    ownerId = 77L,
                    deletedAt = deletedAt,
                    hardDeleteAfter = deletedAt.plusDays(30),
                )

                fileRepository
                    .findById(target.id)
                    .orElseThrow()
                    .isDeleted
                    .shouldBeTrue()
            }

            it("ownerId 목록의 활성 파일을 한 번에 삭제 예약한다") {
                val deletedAt = LocalDateTime.of(2026, 6, 25, 12, 0)
                val hardDeleteAfter = deletedAt.plusDays(30)
                val first =
                    fileRepository.save(createTestFile("first.png", FileOwnerType.COMMENT, 77L, FileStatus.UPLOADED))
                val second =
                    fileRepository.save(createTestFile("second.png", FileOwnerType.COMMENT, 78L, FileStatus.UPLOADED))
                val otherOwner =
                    fileRepository.save(
                        createTestFile("other-owner.png", FileOwnerType.COMMENT, 79L, FileStatus.UPLOADED),
                    )

                val updatedCount =
                    fileRepository.markActiveDeletedByOwnerTypeAndOwnerIdIn(
                        ownerType = FileOwnerType.COMMENT,
                        ownerIds = listOf(77L, 78L),
                        deletedAt = deletedAt,
                        hardDeleteAfter = hardDeleteAfter,
                    )

                updatedCount shouldBe 2
                row(first.id).booleanBy("is_deleted").shouldBeTrue()
                row(first.id).localDateTimeBy("hard_delete_after") shouldBe hardDeleteAfter
                row(first.id).localDateTimeBy("modified_at") shouldBe deletedAt
                row(second.id).booleanBy("is_deleted").shouldBeTrue()
                row(second.id).localDateTimeBy("hard_delete_after") shouldBe hardDeleteAfter
                row(second.id).localDateTimeBy("modified_at") shouldBe deletedAt
                row(otherOwner.id).booleanBy("is_deleted").shouldBeFalse()
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

        describe("jdbc row helpers") {
            it("지원하지 않는 boolean 값은 실패시킨다") {
                shouldThrow<IllegalStateException> {
                    mapOf("is_deleted" to "Y").booleanBy("is_deleted")
                }
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

private fun Map<String, Any?>.booleanBy(key: String): Boolean =
    when (val value = entries.first { it.key.equals(key, ignoreCase = true) }.value) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        else -> error("Unsupported Boolean value: $value")
    }

private fun Map<String, Any?>.localDateTimeBy(key: String): LocalDateTime =
    when (val value = entries.first { it.key.equals(key, ignoreCase = true) }.value) {
        is java.sql.Timestamp -> value.toLocalDateTime()
        is LocalDateTime -> value
        else -> error("Unsupported LocalDateTime value: $value")
    }
