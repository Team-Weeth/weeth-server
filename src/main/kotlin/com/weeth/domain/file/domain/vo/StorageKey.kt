package com.weeth.domain.file.domain.vo

import com.weeth.domain.file.domain.entity.FileOwnerType

@JvmInline
value class StorageKey(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "storageKey는 비어 있을 수 없습니다." }
        require(STORAGE_KEY_PATTERN.matches(value)) {
            "storageKey 형식이 올바르지 않습니다. 형식: {OWNER_TYPE}/{yyyy-MM}/{uuid}_{fileName}"
        }
    }

    companion object {
        private val OWNER_TYPE_PATTERN = FileOwnerType.entries.joinToString("|") { it.name }
        private val STORAGE_KEY_PATTERN =
            Regex(
                pattern = "^($OWNER_TYPE_PATTERN)/(\\d{4}-(0[1-9]|1[0-2]))/([0-9a-fA-F-]{36})_.+$",
            )
    }
}
