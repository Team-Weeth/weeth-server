package com.weeth.domain.notification.infrastructure

import com.weeth.domain.notification.domain.enums.NotificationChannel
import com.weeth.domain.notification.domain.enums.NotificationType
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CodeNotificationTemplateProviderTest :
    StringSpec({
        val provider = CodeNotificationTemplateProvider()
        "공지는 앱 내·푸시 채널 모두 코드 템플릿과 필수 변수를 제공한다" {
            NotificationChannel.entries.forEach { channel ->
                val template = requireNotNull(provider.find(NotificationType.NOTICE_CREATED, channel))
                template.version shouldBe 1
                template.titleTemplate shouldBe "새 공지가 등록되었습니다"
                template.bodyTemplate shouldBe "{noticeTitle}"
                template.requiredVariables shouldBe setOf("noticeTitle")
            }
            provider.find(NotificationType.NOTICE_CREATED, NotificationChannel.PUSH, 99) shouldBe null
        }
    })
