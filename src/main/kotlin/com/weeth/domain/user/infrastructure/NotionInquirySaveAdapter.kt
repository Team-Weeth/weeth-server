package com.weeth.domain.user.infrastructure

import com.weeth.domain.user.domain.port.InquirySavePort
import com.weeth.global.config.properties.NotionProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.LocalDate

@Component
class NotionInquirySaveAdapter(
    private val notionProperties: NotionProperties,
    restClientBuilder: RestClient.Builder,
) : InquirySavePort {
    private val restClient = restClientBuilder.baseUrl("https://api.notion.com").build()
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    override fun save(
        email: String,
        message: String,
    ) {
        val body =
            mapOf(
                "parent" to
                    mapOf(
                        "type" to "database_id",
                        "database_id" to notionProperties.inquiryDatabaseId,
                    ),
                "properties" to
                    mapOf(
                        "문의내용" to
                            mapOf(
                                "title" to listOf(mapOf("text" to mapOf("content" to message))),
                            ),
                        "이메일" to
                            mapOf(
                                "email" to email,
                            ),
                        "날짜" to
                            mapOf(
                                "date" to mapOf("start" to LocalDate.now().toString()),
                            ),
                    ),
            )

        runCatching {
            restClient
                .post()
                .uri("/v1/pages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${notionProperties.token}")
                .header("Notion-Version", notionProperties.version)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(body)
                .retrieve()
                .toBodilessEntity()
        }.onFailure { e -> log.warn("Notion 저장 실패: {}", e.message) }
    }
}
