package com.weeth.domain.user.infrastructure

import com.weeth.domain.user.domain.port.InquiryNotifyPort
import com.weeth.global.config.properties.SlackProperties
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class SlackInquiryNotifyAdapter(
    private val slackProperties: SlackProperties,
    restClientBuilder: RestClient.Builder,
) : InquiryNotifyPort {
    private val restClient = restClientBuilder.build()
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    override fun notify(
        email: String,
        message: String,
    ) {
        val text = "*[랜딩 문의하기]*\n*이메일:* $email\n*문의 내용:*\n```$message```"

        runCatching {
            restClient
                .post()
                .uri(slackProperties.webhookUrl)
                .header("Content-Type", "application/json")
                .body(mapOf("text" to text))
                .retrieve()
                .toBodilessEntity()
        }.onFailure { e -> log.warn("Slack 알림 전송 실패: {}", e.message) }
    }
}
