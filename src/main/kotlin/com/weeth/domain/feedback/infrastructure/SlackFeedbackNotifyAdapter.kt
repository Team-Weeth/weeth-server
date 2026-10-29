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
                "*동아리:* ${escapeForSlack(clubName)}\n" +
                "*작성자:* ${escapeForSlack(authorName)}\n" +
                "*채널:* ${channel.name}\n" +
                "*내용:*\n```${escapeForSlack(content)}```"

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

    /**
     * 사용자 입력을 Slack 메시지에 그대로 꽂아 넣지 않도록 방어한다.
     * `&`/`<`/`>`를 escape해 `<!channel>`, `<!here>`, `<@user>` 같은 멘션/링크 문법을 무력화하고,
     * 연속된 ``` 로 코드 블록을 깨고 뒤 텍스트를 임의로 꾸미는 것을 막는다.
     */
    private fun escapeForSlack(text: String): String =
        text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("```", "` ` `")
}
