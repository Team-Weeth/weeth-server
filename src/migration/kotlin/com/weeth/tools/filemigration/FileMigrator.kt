package com.weeth.tools.filemigration

import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CopyObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.S3Exception
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDateTime

/**
 * V3 → V4 파일 마이그레이터.
 *
 * SQL만으로는 끝나지 않는 부분을 담당한다.
 *  - V4 `file.file_size`/`content_type`은 NOT NULL인데 V3 DB에 값이 없다 → HeadObject로 수집
 *  - 키 형식이 완전히 달라 모든 객체를 새 키로 복사해야 한다 → CopyObject(서버사이드)
 *
 * 원본 버킷은 읽기만 한다. 삭제하지 않으므로 롤백은 V4 쪽만 정리하면 된다.
 */
class FileMigrator(
    private val config: MigrationConfig,
    private val s3: S3Client?,
    private val work: Connection,
    private val target: Connection,
) {
    private data class Pending(
        val v3FileId: Long,
        val oldKey: String,
        val ownerType: String,
        val ownerId: Long,
        val fileName: String,
        val createdAt: LocalDateTime,
    )

    private val stats = linkedMapOf<String, Int>()

    private fun count(key: String) {
        stats[key] = (stats[key] ?: 0) + 1
    }

    fun run() {
        println("=== 파일 마이그레이션 ${if (config.dryRun) "[DRY-RUN]" else "[APPLY]"} ===")
        println("  원본: s3://${config.sourceBucket}")
        println("  대상: s3://${config.targetBucket}")

        val pending = loadPending()
        println("  대상 건수: ${pending.size}")

        pending.forEach { process(it) }

        if (!config.dryRun) {
            val inserted = insertFileRows()
            println("  file 행 생성: $inserted")
        }

        println("\n=== 결과 ===")
        stats.forEach { (k, v) -> println("  %-20s %d".format(k, v)) }
        printSummary()
    }

    // -------------------------------------------------------------------------
    // 1. 대상 조회
    // -------------------------------------------------------------------------
    private fun loadPending(): List<Pending> {
        val limit = if (config.limit > 0) "LIMIT ${config.limit}" else ""
        val sql = """
            SELECT m.v3_file_id, m.old_key, m.owner_type, m.v4_owner_id,
                   f.file_name, f.created_at
            FROM mig_file_map m
                     JOIN ${config.v3Schema}.file f ON f.id = m.v3_file_id
            WHERE m.status = 'PENDING'
            ORDER BY m.v3_file_id
            $limit
        """
        return work.createStatement().use { st ->
            st.executeQuery(sql).use { rs ->
                buildList {
                    while (rs.next()) {
                        add(
                            Pending(
                                v3FileId = rs.getLong(1),
                                oldKey = rs.getString(2),
                                ownerType = rs.getString(3),
                                ownerId = rs.getLong(4),
                                fileName = rs.getString(5) ?: "",
                                createdAt = rs.getTimestamp(6).toLocalDateTime(),
                            ),
                        )
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // 2. 건별 처리: HeadObject → 키 생성 → CopyObject
    // -------------------------------------------------------------------------
    private fun process(item: Pending) {
        if (config.offline) {
            planOffline(item)
            return
        }
        val head =
            try {
                s3!!.headObject(
                    HeadObjectRequest
                        .builder()
                        .bucket(config.sourceBucket)
                        .key(item.oldKey)
                        .build(),
                )
            } catch (e: NoSuchKeyException) {
                mark(item, "MISSING_IN_S3", "S3에 객체 없음")
                count("MISSING_IN_S3")
                return
            } catch (e: S3Exception) {
                // 404는 NoSuchKey가 아니라 S3Exception으로 오는 경우가 있다(권한에 따라 다름).
                if (e.statusCode() == 404) {
                    mark(item, "MISSING_IN_S3", "S3에 객체 없음(404)")
                    count("MISSING_IN_S3")
                } else {
                    mark(item, "FAILED", "HeadObject 실패: ${e.statusCode()}")
                    count("HEAD_FAILED")
                }
                return
            }

        val size = head.contentLength() ?: 0L
        if (size <= 0L) {
            // V4 File.create 가 require(fileSize > 0) 이므로 0바이트는 넣을 수 없다.
            mark(item, "FAILED", "0바이트 객체")
            count("ZERO_BYTE")
            return
        }

        val ext = StorageKeyFactory.extensionOf(item.oldKey)
        val contentType =
            ContentTypes.fromExtension(ext)
                ?: head.contentType()?.takeIf { ContentTypes.isSupported(it) }
        if (contentType == null) {
            mark(item, "UNSUPPORTED_TYPE", "확장자: ${ext ?: "불명"}")
            count("UNSUPPORTED_TYPE")
            return
        }

        val newKey = StorageKeyFactory.create(item.ownerType, item.createdAt, item.oldKey, item.fileName)
        if (newKey == null) {
            mark(item, "FAILED", "StorageKey 생성 실패(비표준 키)")
            count("KEY_FAILED")
            return
        }

        if (config.dryRun) {
            println("  [계획] ${item.oldKey} -> $newKey  (${size}B, $contentType)")
            count("PLANNED")
            return
        }

        try {
            s3!!.copyObject(
                CopyObjectRequest
                    .builder()
                    .sourceBucket(config.sourceBucket)
                    .sourceKey(item.oldKey)
                    .destinationBucket(config.targetBucket)
                    .destinationKey(newKey)
                    .build(),
            )
        } catch (e: S3Exception) {
            mark(item, "FAILED", "CopyObject 실패: ${e.statusCode()}")
            count("COPY_FAILED")
            return
        }

        work
            .prepareStatement(
                """
            UPDATE mig_file_map
            SET new_key = ?, file_size = ?, content_type = ?, status = 'COPIED', note = NULL
            WHERE v3_file_id = ?
            """,
            ).use {
                it.setString(1, newKey)
                it.setLong(2, size)
                it.setString(3, contentType)
                it.setLong(4, item.v3FileId)
                it.executeUpdate()
            }
        count("COPIED")
    }

    /**
     * S3 없이 키 생성과 소유 대상 매핑만 검토한다.
     * file_size는 HeadObject 없이는 알 수 없으므로 표시하지 않는다.
     */
    private fun planOffline(item: Pending) {
        val ext = StorageKeyFactory.extensionOf(item.oldKey)
        val contentType = ContentTypes.fromExtension(ext)
        if (contentType == null) {
            println("  [스킵] ${item.oldKey} -> UNSUPPORTED_TYPE (확장자: ${ext ?: "불명"})")
            count("UNSUPPORTED_TYPE")
            return
        }
        val newKey = StorageKeyFactory.create(item.ownerType, item.createdAt, item.oldKey, item.fileName)
        if (newKey == null) {
            println("  [스킵] ${item.oldKey} -> KEY_FAILED (비표준 키)")
            count("KEY_FAILED")
            return
        }
        println("  [계획] owner=${item.ownerType}#${item.ownerId}  $contentType")
        println("         ${item.oldKey}")
        println("      -> $newKey")
        count("PLANNED")
    }

    private fun mark(
        item: Pending,
        status: String,
        note: String,
    ) {
        if (config.dryRun) {
            println("  [스킵] ${item.oldKey} -> $status ($note)")
            return
        }
        work.prepareStatement("UPDATE mig_file_map SET status = ?, note = ? WHERE v3_file_id = ?").use {
            it.setString(1, status)
            it.setString(2, note)
            it.setLong(3, item.v3FileId)
            it.executeUpdate()
        }
    }

    // -------------------------------------------------------------------------
    // 3. V4 file 행 생성
    // -------------------------------------------------------------------------

    /**
     * 복사가 끝난 건을 V4 `file`에 INSERT한다.
     * storage_key가 UNIQUE이므로 중복 삽입은 자연히 차단된다.
     */
    private fun insertFileRows(): Int {
        val rows =
            work.createStatement().use { st ->
                st
                    .executeQuery(
                        """
                    SELECT m.v3_file_id, m.new_key, m.file_size, m.content_type,
                           m.owner_type, m.v4_owner_id, f.file_name, f.created_at, f.modified_at
                    FROM mig_file_map m
                             JOIN ${config.v3Schema}.file f ON f.id = m.v3_file_id
                    WHERE m.status = 'COPIED'
                    ORDER BY m.v3_file_id
                    """,
                    ).use { rs ->
                        buildList {
                            while (rs.next()) {
                                add(
                                    arrayOf(
                                        rs.getLong(1),
                                        rs.getString(2),
                                        rs.getLong(3),
                                        rs.getString(4),
                                        rs.getString(5),
                                        rs.getLong(6),
                                        rs.getString(7),
                                        rs.getTimestamp(8),
                                        rs.getTimestamp(9),
                                    ),
                                )
                            }
                        }
                    }
            }

        var inserted = 0
        target
            .prepareStatement(
                """
            INSERT IGNORE INTO file (created_at, modified_at, file_name, storage_key,
                                     file_size, owner_type, owner_id, content_type, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'UPLOADED')
            """,
            ).use { ps ->
                rows.forEach { r ->
                    ps.setTimestamp(1, r[7] as java.sql.Timestamp)
                    ps.setTimestamp(2, r[8] as java.sql.Timestamp)
                    ps.setString(3, r[6] as String)
                    ps.setString(4, r[1] as String)
                    ps.setLong(5, r[2] as Long)
                    ps.setString(6, r[4] as String)
                    ps.setLong(7, r[5] as Long)
                    ps.setString(8, r[3] as String)
                    inserted += ps.executeUpdate()
                }
            }

        work.createStatement().use {
            it.executeUpdate("UPDATE mig_file_map SET status = 'INSERTED' WHERE status = 'COPIED'")
        }
        return inserted
    }

    private fun printSummary() {
        work.createStatement().use { st ->
            st.executeQuery("SELECT status, COUNT(*) FROM mig_file_map GROUP BY status ORDER BY 2 DESC").use { rs ->
                println("\n  mig_file_map 현황")
                while (rs.next()) println("    %-20s %d".format(rs.getString(1), rs.getInt(2)))
            }
        }
    }
}

fun main(args: Array<String>) {
    val config = MigrationConfig.fromArgs(args)

    // 오프라인 모드에서는 S3Client를 만들지 않는다(자격증명 없이도 실행되어야 한다).
    val s3 = if (config.offline) null else S3Client.builder().region(Region.of(config.region)).build()
    try {
        DriverManager.getConnection(config.workJdbcUrl, config.dbUser, config.dbPassword).use { work ->
            DriverManager.getConnection(config.targetJdbcUrl, config.dbUser, config.dbPassword).use { target ->
                FileMigrator(config, s3, work, target).run()
            }
        }
    } finally {
        s3?.close()
    }
}
