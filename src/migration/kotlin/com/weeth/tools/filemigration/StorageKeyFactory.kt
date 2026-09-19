package com.weeth.tools.filemigration

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * V3 평면 키를 V4 StorageKey 형식으로 변환한다.
 *
 * V3: `{uuid}.{ext}`                              — 버킷 루트
 * V4: `{OWNER_TYPE}/{yyyy-MM}/{uuid}_{fileName}`   — StorageKey VO가 정규식으로 강제
 *
 * uuid는 V3 키의 것을 그대로 재사용한다. 같은 입력이면 같은 키가 나오므로
 * 재실행해도 동일한 대상에 덮어쓰기만 발생한다(멱등).
 */
object StorageKeyFactory {
    private val MONTH = DateTimeFormatter.ofPattern("yyyy-MM")
    private val V3_KEY = Regex("^([0-9a-fA-F-]{36})\\.([A-Za-z0-9]+)$")
    private const val MAX_LENGTH = 500

    /** V4 StorageKey 정규식과 동일한 검증. 생성 결과를 여기서 한 번 더 확인한다. */
    private val V4_KEY =
        Regex(
            "^(POST|COMMENT|ACCOUNT_TRANSACTION|CLUB_MEMBER_PROFILE|USER_PROFILE_IMAGE|" +
                "USER_PROFILE_HEADER|CLUB_PROFILE|CLUB_BACKGROUND)/" +
                "(\\d{4}-(0[1-9]|1[0-2]))/([0-9a-fA-F-]{36})_.+$",
        )

    fun parseUuid(oldKey: String): String? = V3_KEY.find(oldKey)?.groupValues?.get(1)

    fun extensionOf(oldKey: String): String? =
        V3_KEY
            .find(oldKey)
            ?.groupValues
            ?.get(2)
            ?.lowercase()

    /**
     * @return 생성된 키. V3 키가 비표준이거나 결과가 V4 형식을 만족하지 못하면 null.
     */
    fun create(
        ownerType: String,
        createdAt: LocalDateTime,
        oldKey: String,
        fileName: String,
    ): String? {
        val uuid = parseUuid(oldKey) ?: return null

        // V3는 원본 파일명을 보존하지 않아 file_name에 S3 키({uuid}.{ext})가 그대로 들어 있다.
        // 그대로 쓰면 키에 uuid가 두 번 들어가므로(`{uuid}_{uuid}.ext`) 확장자만 남긴다.
        val ext = extensionOf(oldKey)
        val displayName =
            when {
                fileName.isBlank() -> "file.${ext ?: "bin"}"
                fileName.startsWith(uuid) -> "file.${ext ?: "bin"}"
                else -> fileName
            }

        val key = "$ownerType/${createdAt.format(MONTH)}/${uuid}_$displayName"
        if (key.length > MAX_LENGTH) return null
        return key.takeIf { V4_KEY.matches(it) }
    }
}
