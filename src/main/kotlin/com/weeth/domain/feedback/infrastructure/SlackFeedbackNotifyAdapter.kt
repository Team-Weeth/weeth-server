package com.weeth.domain.feedback.infrastructure

import com.weeth.domain.feedback.domain.enums.FeedbackChannel
import com.weeth.domain.feedback.domain.port.FeedbackNotifyPort
import com.weeth.global.config.properties.SlackProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class SlackFeedbackNotifyAdapter(
    private val slackProperties: SlackProperties,
    restClientBuilder: RestClient.Builder,
) : FeedbackNotifyPort {
    private val restClient = restClientBuilder.build()
    private val log = LoggerFactory.getLogger(javaClass)

    override fun notify(
        clubName: String,
        authorName: String,
        channel: FeedbackChannel,
        content: String,
    ) {
        val text =
            "*[서비스 피드백]*\n" +
                "*동아리:* $clubName\n" +
                "*작성자:* $authorName\n" +
                "*채널:* ${channel.name}\n" +
                "*내용:*\n```$content```"

        runCatching {
            restClient
                .post()
                .uri(slackProperties.webhookUrl)
                .header("Content-Type", "application/json")
                .body(mapOf("text" to text))
                .retrieve()
                .toBodilessEntity()
        }.onFailure { e -> log.warn("Slack 피드백 알림 전송 실패: {}", e.message) }
    }
}
