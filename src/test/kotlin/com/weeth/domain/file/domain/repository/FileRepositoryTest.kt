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
            it("기본 단건 owner 조회는 업로드 상태인 파일만 반환한다") {
                fileRepository.save(createTestFile("target.png", FileOwnerType.COMMENT, 77L, FileStatus.UPLOADED))
                fileRepository.save(
                    createTestFile("status-deleted.png", FileOwnerType.COMMENT, 77L, FileStatus.DELETED),
                )

                val files = fileRepository.findAll(FileOwnerType.COMMENT, 77L)

                files.map { it.fileName } shouldContainExactly listOf("target.png")
            }

            it("기본 ownerId 목록 조회는 업로드 상태인 파일만 반환한다") {
                fileRepository.save(createTestFile("target-1.png", FileOwnerType.POST, 77L, FileStatus.UPLOADED))
                fileRepository.save(createTestFile("target-2.png", FileOwnerType.POST, 78L, FileStatus.UPLOADED))
                fileRepository.save(createTestFile("status-deleted.png", FileOwnerType.POST, 78L, FileStatus.DELETED))

                val files = fileRepository.findAll(FileOwnerType.POST, listOf(77L, 78L))

                files.map { it.fileName }.sorted() shouldContainExactly listOf("target-1.png", "target-2.png")
            }

            it("ownerType + ownerId + status 조건에 맞는 데이터만 조회한다") {
                fileRepository.save(createTestFile("target-1.png", FileOwnerType.POST, 77L, FileStatus.UPLOADED))
                fileRepository.save(createTestFile("target-2.png", FileOwnerType.POST, 77L, FileStatus.UPLOADED))
                fileRepository.save(createTestFile("deleted.png", FileOwnerType.POST, 77L, FileStatus.DELETED))
                fileRepository.save(createTestFile("other-owner.png", FileOwnerType.POST, 78L, FileStatus.UPLOADED))
                fileRepository.save(createTestFile("other-type.png", FileOwnerType.COMMENT, 77L, FileStatus.UPLOADED))

                val uploaded = fileRepository.findAll(FileOwnerType.POST, 77L, FileStatus.UPLOADED)
                val allStatus = fileRepository.findAll(FileOwnerType.POST, 77L, null)

                uploaded.map { it.fileName }.sorted() shouldContainExactly listOf("target-1.png", "target-2.png")
                allStatus.map { it.fileName }.sorted() shouldContainExactly
                    listOf("deleted.png", "target-1.png", "target-2.png")

                fileRepository.exists(FileOwnerType.POST, 77L, FileStatus.UPLOADED).shouldBeTrue()
                fileRepository.exists(FileOwnerType.POST, 77L, FileStatus.DELETED).shouldBeTrue()
                fileRepository.exists(FileOwnerType.POST, 99L, FileStatus.UPLOADED).shouldBeFalse()
            }

            it("ownerId 목록 조회에서도 업로드 상태인 파일만 반환한다") {
                fileRepository.save(createTestFile("target-1.png", FileOwnerType.POST, 77L, FileStatus.UPLOADED))
                fileRepository.save(createTestFile("target-2.png", FileOwnerType.POST, 78L, FileStatus.UPLOADED))

                val files = fileRepository.findAll(FileOwnerType.POST, listOf(77L, 78L), FileStatus.UPLOADED)

                files.map { it.fileName }.sorted() shouldContainExactly listOf("target-1.png", "target-2.png")
            }
        }

        describe("hard delete") {
            it("삭제 후 같은 storageKey를 다시 저장할 수 있다") {
                val storageKey = "POST/2026-02/550e8400-e29b-41d4-a716-446655440000_same.png"
                fileRepository.saveAndFlush(
                    File.createUploaded(
                        fileName = "same.png",
                        storageKey = storageKey,
                        fileSize = 1024L,
                        contentType = "image/png",
                        ownerType = FileOwnerType.POST,
                        ownerId = 77L,
                    ),
                )

                val deletedCount = fileRepository.hardDeleteActiveByOwnerTypeAndOwnerId(FileOwnerType.POST, 77L)

                val recreated =
                    fileRepository.saveAndFlush(
                        File.createUploaded(
                            fileName = "same.png",
                            storageKey = storageKey,
                            fileSize = 1024L,
                            contentType = "image/png",
                            ownerType = FileOwnerType.POST,
                            ownerId = 77L,
                        ),
                    )

                deletedCount shouldBe 1
                recreated.storageKey.value shouldBe storageKey
            }

            it("ownerId 목록의 업로드 상태 파일을 한 번에 삭제한다") {
                fileRepository.save(createTestFile("target-1.png", FileOwnerType.POST, 77L, FileStatus.UPLOADED))
                fileRepository.save(createTestFile("target-2.png", FileOwnerType.POST, 78L, FileStatus.UPLOADED))
                fileRepository.save(createTestFile("deleted.png", FileOwnerType.POST, 78L, FileStatus.DELETED))
                fileRepository.save(createTestFile("other-owner.png", FileOwnerType.POST, 79L, FileStatus.UPLOADED))
                fileRepository.flush()

                val deletedCount =
                    fileRepository.hardDeleteActiveByOwnerTypeAndOwnerIdIn(FileOwnerType.POST, listOf(77L, 78L))

                deletedCount shouldBe 2
                fileRepository.exists(FileOwnerType.POST, 77L, FileStatus.UPLOADED).shouldBeFalse()
                fileRepository.exists(FileOwnerType.POST, 78L, FileStatus.UPLOADED).shouldBeFalse()
                fileRepository.exists(FileOwnerType.POST, 78L, FileStatus.DELETED).shouldBeTrue()
                fileRepository.exists(FileOwnerType.POST, 79L, FileStatus.UPLOADED).shouldBeTrue()
            }
        }

        describe("index usage") {
            it("owner_type + owner_id 조건 조회 시 복합 인덱스를 사용한다") {
                fileRepository.save(createTestFile("index-target.png", FileOwnerType.COMMENT, 55L, FileStatus.UPLOADED))

                val explain =
                    jdbcTemplate
                        .queryForList(
                            "EXPLAIN SELECT id FROM `file` WHERE owner_type = ? AND owner_id = ?",
                            FileOwnerType.COMMENT.name,
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
        }

private fun Map<String, Any?>.valueBy(key: String): String =
    entries
        .first {
            it.key.equals(key, ignoreCase = true)
        }.value
        .toString()
