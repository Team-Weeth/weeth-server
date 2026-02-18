package com.weeth.domain.file.infrastructure

import com.weeth.domain.file.domain.port.FileAccessUrlPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "app.file",
    name = ["url-provider"],
    havingValue = "CDN", // CDN으로 설정된 경우 이 어댑터를 사용합니다.
)
class CdnFileAccessUrlAdapter(
    @Value("\${app.file.cdn-base-url:}") private val cdnBaseUrl: String,
) : FileAccessUrlPort {
    /** storageKey를 CDN 조회 URL로 변환합니다. */
    override fun resolve(storageKey: String): String {
        val normalizedBaseUrl = cdnBaseUrl.trimEnd('/')
        return if (normalizedBaseUrl.isBlank()) {
            storageKey
        } else {
            "$normalizedBaseUrl/$storageKey"
        }
    }
}
