package com.weeth.global.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.LocalDateTime

/**
 * v3 에디터(마크다운)로 작성된 게시글 본문을 v4 Tiptap HTML로 변환해 내려주기 위한 설정.
 *
 * [editorMigratedAt] 이 설정된 환경에서만 변환이 동작한다. 변환 대상을 출처로 못박기 위한 값이며,
 * 본문 내용만으로는 v4에서 작성한 `- 대시 문장`과 마크다운 리스트를 구분할 수 없어
 * 범위를 좁히지 않으면 정상 글까지 변형된다.
 */
@ConfigurationProperties(prefix = "legacy-content")
data class LegacyContentProperties(
    /**
     * v3에서 이관된 게시글이 속한 동아리 TSID.
     *
     * 지정하면 해당 동아리 글만 변환한다. 비우면 동아리를 가리지 않고 [editorMigratedAt] 이전 글을
     * 모두 변환하므로, 동아리 TSID가 환경마다 달라 값을 특정하기 어려운 dev 에서만 비워 둔다.
     */
    val clubId: Long? = null,
    /** v4 에디터 전환 시각. 이 시각 이전에 작성된 글만 변환한다. 없으면 변환하지 않는다. */
    val editorMigratedAt: LocalDateTime? = null,
) {
    fun isEnabled(): Boolean = editorMigratedAt != null
}
