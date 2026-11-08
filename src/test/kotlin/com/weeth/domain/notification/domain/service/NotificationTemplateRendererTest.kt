package com.weeth.domain.notification.domain.service

import com.weeth.domain.notification.domain.enums.NotificationChannel
import com.weeth.domain.notification.domain.enums.NotificationTemplateFailureReason
import com.weeth.domain.notification.domain.enums.NotificationType
import com.weeth.domain.notification.domain.port.NotificationTemplateProvider
import com.weeth.domain.notification.domain.vo.NotificationRenderingResult
import com.weeth.domain.notification.domain.vo.NotificationTemplate
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class NotificationTemplateRendererTest :
    StringSpec({
        val provider = mockk<NotificationTemplateProvider>()
        val renderer = NotificationTemplateRenderer(provider)
        beforeTest { clearMocks(provider) }

        "유형과 채널별 템플릿을 적용하고 본문을 255자로 축약한다" {
            every { provider.find(NotificationType.NOTICE_CREATED, NotificationChannel.IN_APP, null) } returns
                NotificationTemplate(1, "앱 내 공지", "{noticeTitle}", setOf("noticeTitle"))
            every { provider.find(NotificationType.NOTICE_CREATED, NotificationChannel.PUSH, null) } returns
                NotificationTemplate(2, "푸시 공지", "{noticeTitle}", setOf("noticeTitle"))
            val app =
                renderer.render(
                    NotificationType.NOTICE_CREATED,
                    NotificationChannel.IN_APP,
                    mapOf("noticeTitle" to "가".repeat(300)),
                ) as NotificationRenderingResult.Success
            val push =
                renderer.render(
                    NotificationType.NOTICE_CREATED,
                    NotificationChannel.PUSH,
                    mapOf("noticeTitle" to "공지"),
                ) as NotificationRenderingResult.Success
            app.message.title shouldBe "앱 내 공지"
            app.message.body.length shouldBe 255
            app.message.body.last() shouldBe '…'
            app.message.templateVersion shouldBe 1
            push.message.title shouldBe "푸시 공지"
            push.message.templateVersion shouldBe 2
        }

        "필수 변수 누락과 null 및 공백은 원문 없이 실패 이유와 변수명으로 반환한다" {
            every { provider.find(any(), any(), any()) } returns
                NotificationTemplate(1, "공지", "{noticeTitle}", setOf("noticeTitle"))
            listOf(emptyMap(), mapOf("noticeTitle" to null), mapOf("noticeTitle" to "　 \t")).forEach { variables ->
                val result =
                    renderer.render(
                        NotificationType.NOTICE_CREATED,
                        NotificationChannel.PUSH,
                        variables,
                    ) as NotificationRenderingResult.Failure
                result.reason shouldBe NotificationTemplateFailureReason.MISSING_VARIABLES
                result.missingVariables shouldBe setOf("noticeTitle")
                result.templateVersion shouldBe 1
            }
        }

        "사용자 변수에 포함된 placeholder는 다시 해석하지 않는다" {
            every { provider.find(any(), any(), any()) } returns
                NotificationTemplate(1, "공지", "{noticeTitle}", setOf("noticeTitle"))
            val result =
                renderer.render(
                    NotificationType.NOTICE_CREATED,
                    NotificationChannel.PUSH,
                    mapOf("noticeTitle" to "{privateValue} \\$1", "privateValue" to "노출 금지"),
                ) as NotificationRenderingResult.Success
            result.message.body shouldBe "{privateValue} \\$1"
        }

        "템플릿 부재와 선언하지 않은 placeholder는 실패한다" {
            every { provider.find(any(), any(), any()) } returns null
            (
                renderer.render(
                    NotificationType.NOTICE_CREATED,
                    NotificationChannel.PUSH,
                    emptyMap(),
                ) as NotificationRenderingResult.Failure
            ).reason shouldBe
                NotificationTemplateFailureReason.TEMPLATE_NOT_FOUND
            every { provider.find(any(), any(), any()) } returns
                NotificationTemplate(1, "공지", "{undeclared}", emptySet())
            (
                renderer.render(
                    NotificationType.NOTICE_CREATED,
                    NotificationChannel.PUSH,
                    emptyMap(),
                ) as NotificationRenderingResult.Failure
            ).reason shouldBe
                NotificationTemplateFailureReason.INVALID_TEMPLATE
        }

        "제목 100자 초과 및 공백 본문은 실패한다" {
            every { provider.find(any(), any(), any()) } returns
                NotificationTemplate(1, "가".repeat(101), "본문", emptySet())
            (
                renderer.render(
                    NotificationType.NOTICE_CREATED,
                    NotificationChannel.PUSH,
                    emptyMap(),
                ) as NotificationRenderingResult.Failure
            ).reason shouldBe
                NotificationTemplateFailureReason.INVALID_MESSAGE
            every { provider.find(any(), any(), any()) } returns NotificationTemplate(1, "공지", " ", emptySet())
            (
                renderer.render(
                    NotificationType.NOTICE_CREATED,
                    NotificationChannel.PUSH,
                    emptyMap(),
                ) as NotificationRenderingResult.Failure
            ).reason shouldBe
                NotificationTemplateFailureReason.INVALID_MESSAGE
        }
    })
