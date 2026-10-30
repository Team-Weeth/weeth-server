package com.weeth.domain.feedback.infrastructure

import com.weeth.domain.feedback.domain.enums.FeedbackChannel
import com.weeth.global.config.properties.SlackProperties
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestClient

class SlackFeedbackNotifyAdapterTest :
    DescribeSpec({
        val slackProperties = SlackProperties(webhookUrl = "https://hooks.slack.test/webhook")
        val builder = mockk<RestClient.Builder>()
        val restClient = mockk<RestClient>()
        val requestBodyUriSpec = mockk<RestClient.RequestBodyUriSpec>()
        val requestBodySpec = mockk<RestClient.RequestBodySpec>()
        val responseSpec = mockk<RestClient.ResponseSpec>()
        val bodySlot = slot<Map<String, String>>()

        every { builder.build() } returns restClient
        every { restClient.post() } returns requestBodyUriSpec
        every { requestBodyUriSpec.uri(any<String>()) } returns requestBodySpec
        every { requestBodySpec.header(any(), any()) } returns requestBodySpec
        every { requestBodySpec.body(capture(bodySlot)) } returns requestBodySpec
        every { requestBodySpec.retrieve() } returns responseSpec
        every { responseSpec.toBodilessEntity() } returns ResponseEntity.ok().build()

        val adapter = SlackFeedbackNotifyAdapter(slackProperties, builder)

        describe("notify") {
            it("클럽명/작성자명/내용의 &, <, >를 escape해 멘션·링크 문법을 무력화한다") {
                adapter.notify(
                    clubName = "위드 & 친구들",
                    authorName = "<!channel> 홍길동",
                    channel = FeedbackChannel.CHAT,
                    content = "<script>alert(1)</script> & <@U123> 멘션 테스트",
                )

                val text = bodySlot.captured["text"].orEmpty()
                text shouldNotContain "<!channel>"
                text shouldNotContain "<script>"
                text shouldNotContain "<@U123>"
                text shouldContain "&amp;"
                text shouldContain "&lt;!channel&gt;"
                text shouldContain "&lt;script&gt;alert(1)&lt;/script&gt;"
                text shouldContain "&lt;@U123&gt;"
            }

            it("내용에 포함된 ```를 무력화해 코드 블록을 깨고 메시지를 조작하지 못하게 한다") {
                adapter.notify(
                    clubName = "위드",
                    authorName = "홍길동",
                    channel = FeedbackChannel.FORM,
                    content = "정상 내용```*[가짜 공지]* 아무 말",
                )

                val text = bodySlot.captured["text"].orEmpty()
                text shouldNotContain "```*[가짜 공지]*"
                text shouldContain "` ` `"
            }
        }
    })
