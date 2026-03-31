package com.weeth.domain.user.infrastructure

import com.weeth.domain.user.application.exception.NotionApiException
import com.weeth.domain.user.domain.port.InquirySavePort
import com.weeth.global.config.properties.NotionProperties
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.LocalDate

@Component
class NotionInquirySaveAdapter(
    private val notionProperties: NotionProperties,
    restClientBuilder: RestClient.Builder,
) : InquirySavePort {
    private val restClient = restClientBuilder.baseUrl("https://api.notion.com").build()

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
        }.getOrElse { e -> throw NotionApiException(e) }
    }
}
