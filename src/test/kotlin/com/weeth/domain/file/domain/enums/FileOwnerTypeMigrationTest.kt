package com.weeth.domain.file.domain.enums

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainAll
import java.nio.file.Files
import java.nio.file.Path

class FileOwnerTypeMigrationTest :
    DescribeSpec({
        describe("file.owner_type 마이그레이션") {
            it("MySQL ENUM 정의는 FileOwnerType 값을 모두 포함한다") {
                val latestEnumValues = latestOwnerTypeEnumValues()

                latestEnumValues shouldContainAll FileOwnerType.entries.map { it.name }
            }
        }
    })

private fun latestOwnerTypeEnumValues(): List<String> {
    val enumPattern = Regex("""(?is)MODIFY\s+COLUMN\s+owner_type\s+ENUM\s*\((.*?)\)""")
    val enumValuePattern = Regex("""'([^']+)'""")

    return Files
        .list(Path.of("src/main/resources/db/migration"))
        .use { paths -> paths.toList() }
        .filter { it.fileName.toString().endsWith(".sql") }
        .sortedBy { it.flywayVersion() }
        .mapNotNull { path -> enumPattern.find(Files.readString(path))?.groupValues?.get(1) }
        .last()
        .let { enumValues -> enumValuePattern.findAll(enumValues).map { it.groupValues[1] }.toList() }
}

private fun Path.flywayVersion(): Int =
    fileName
        .toString()
        .substringAfter("V")
        .substringBefore("__")
        .toInt()
