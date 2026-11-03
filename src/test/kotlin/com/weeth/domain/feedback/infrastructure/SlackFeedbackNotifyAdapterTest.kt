package com.weeth.domain.feedback.infrastructure

import com.weeth.domain.feedback.domain.enums.FeedbackChannel
import com.weeth.global.config.properties.SlackProperties
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestClient
import java.time.Duration

class SlackFeedbackNotifyAdapterTest :
    DescribeSpec({
        val slackProperties = SlackProperties(webhookUrl = "https://hooks.slack.test/webhook")
        val builder = mockk<RestClient.Builder>()
        val restClient = mockk<RestClient>()
        val requestBodyUriSpec = mockk<RestClient.RequestBodyUriSpec>()
        val requestBodySpec = mockk<RestClient.RequestBodySpec>()
        val responseSpec = mockk<RestClient.ResponseSpec>()
        val bodySlot = slot<Map<String, String>>()

        every { builder.requestFactory(any()) } returns builder
        every { builder.build() } returns restClient
        every { restClient.post() } returns requestBodyUriSpec
        every { requestBodyUriSpec.uri(any<String>()) } returns requestBodySpec
        every { requestBodySpec.header(any(), any()) } returns requestBodySpec
        every { requestBodySpec.body(capture(bodySlot)) } returns requestBodySpec
        every { requestBodySpec.retrieve() } returns responseSpec
        every { responseSpec.toBodilessEntity() } returns ResponseEntity.ok().build()

        val adapter = SlackFeedbackNotifyAdapter(slackProperties, builder)

        describe("생성") {
            it("연결 3초·읽기 5초 타임아웃이 설정된 전용 requestFactory로 RestClient를 구성한다") {
                verify(exactly = 1) { builder.requestFactory(any()) }
                SlackFeedbackNotifyAdapter.REQUEST_FACTORY_SETTINGS.connectTimeout() shouldBe Duration.ofSeconds(3)
                SlackFeedbackNotifyAdapter.REQUEST_FACTORY_SETTINGS.readTimeout() shouldBe Duration.ofSeconds(5)
            }
        }

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

            it("동아리명/작성자명의 줄바꿈을 공백으로 바꿔 가짜 항목 행을 만들지 못하게 한다") {
                adapter.notify(
                    clubName = "위드\n*작성자:* 운영진",
                    authorName = "홍길동\r\n*채널:* 공지",
                    channel = FeedbackChannel.CHAT,
                    content = "첫 줄\n둘째 줄",
                )

                val text = bodySlot.captured["text"].orEmpty()
                text shouldContain "*동아리:* 위드 *작성자:* 운영진\n"
                text shouldContain "*작성자:* 홍길동 *채널:* 공지\n"
                text shouldContain "첫 줄\n둘째 줄"
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
