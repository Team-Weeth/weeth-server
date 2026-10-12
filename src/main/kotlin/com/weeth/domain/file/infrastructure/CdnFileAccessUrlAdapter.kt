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
    @Value("\${app.file.cdn-base-url:}") cdnBaseUrl: String,
) : FileAccessUrlPort {
    // base URL 누락 시 storageKey만 담긴 깨진 URL이 응답되므로 기동 시점에 실패시킨다
    private val baseUrl =
        cdnBaseUrl.trim().trimEnd('/').also {
            require(it.isNotBlank()) { "app.file.url-provider=CDN이면 app.file.cdn-base-url(CDN_BASE_URL)을 설정해야 합니다." }
        }

    /** storageKey를 CDN 조회 URL로 변환합니다. */
    override fun resolve(storageKey: String): String = "$baseUrl/$storageKey"
}
