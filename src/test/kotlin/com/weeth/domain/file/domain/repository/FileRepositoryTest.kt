package com.weeth.domain.file.domain.repository

import com.weeth.config.TestContainersConfig
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.entity.FileOwnerType
import com.weeth.domain.file.domain.entity.FileStatus
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
                            ownerType = FileOwnerType.NOTICE,
                            ownerId = 101L,
                            status = FileStatus.UPLOADED,
                        ),
                    )

                val found = fileRepository.findById(saved.id).orElseThrow()

                found.fileName shouldBe "notice-image.png"
                found.ownerType shouldBe FileOwnerType.NOTICE
                found.ownerId shouldBe 101L
                found.status shouldBe FileStatus.UPLOADED
            }
        }

        describe("findAll/exists") {
            it("ownerType + ownerId + status 조건에 맞는 데이터만 조회한다") {
                fileRepository.save(createTestFile("target-1.png", FileOwnerType.POST, 77L, FileStatus.UPLOADED))
                fileRepository.save(createTestFile("target-2.png", FileOwnerType.POST, 77L, FileStatus.UPLOADED))
                fileRepository.save(createTestFile("deleted.png", FileOwnerType.POST, 77L, FileStatus.DELETED))
                fileRepository.save(createTestFile("other-owner.png", FileOwnerType.POST, 78L, FileStatus.UPLOADED))
                fileRepository.save(createTestFile("other-type.png", FileOwnerType.NOTICE, 77L, FileStatus.UPLOADED))

                val uploaded = fileRepository.findAll(FileOwnerType.POST, 77L, FileStatus.UPLOADED)
                val allStatus = fileRepository.findAll(FileOwnerType.POST, 77L, null)

                uploaded.map { it.fileName }.sorted() shouldContainExactly listOf("target-1.png", "target-2.png")
                allStatus.map { it.fileName }.sorted() shouldContainExactly
                    listOf("deleted.png", "target-1.png", "target-2.png")

                fileRepository.exists(FileOwnerType.POST, 77L, FileStatus.UPLOADED).shouldBeTrue()
                fileRepository.exists(FileOwnerType.POST, 77L, FileStatus.DELETED).shouldBeTrue()
                fileRepository.exists(FileOwnerType.POST, 99L, FileStatus.UPLOADED).shouldBeFalse()
            }
        }

        describe("index usage") {
            it("owner_type + owner_id 조건 조회 시 복합 인덱스를 사용한다") {
                fileRepository.save(createTestFile("index-target.png", FileOwnerType.RECEIPT, 55L, FileStatus.UPLOADED))

                val explain =
                    jdbcTemplate.queryForList(
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
): File =
    File.createUploaded(
        fileName = fileName,
        storageKey = "${ownerType.name}/2026-02/${UUID.randomUUID()}_$fileName",
        fileSize = 1024L,
        contentType = "image/png",
        ownerType = ownerType,
        ownerId = ownerId,
    ).also {
        if (status == FileStatus.DELETED) {
            it.markDeleted()
        }
    }

private fun Map<String, Any?>.valueBy(key: String): String =
    entries.first { it.key.equals(key, ignoreCase = true) }.value.toString()
