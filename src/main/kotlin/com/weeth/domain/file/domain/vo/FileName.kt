package com.weeth.domain.file.domain.vo

class FileName(
    value: String,
) {
    val sanitized: String
    val extension: FileExtension

    init {
        val normalized = value.trim()
        require(normalized.isNotBlank()) { "fileName은 비어 있을 수 없습니다." }

        val ext = normalized.substringAfterLast('.', "")
        require(ext.isNotBlank()) { "fileName에는 확장자가 포함되어야 합니다." }

        extension = FileExtension(ext)
        sanitized = normalized.replace(Regex("""[\\/:*?"<>|]"""), "_")
    }
}
