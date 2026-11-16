package com.weeth.domain.notification

import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.notification.application.event.CreateNoticeNotificationEventListener
import com.weeth.domain.notification.application.usecase.command.CreateNotificationUseCase
import com.weeth.domain.notification.application.usecase.command.ManageNotificationTokenUseCase
import com.weeth.domain.notification.application.usecase.command.ManageNotificationUseCase
import com.weeth.domain.notification.application.usecase.query.GetNotificationQueryService
import com.weeth.domain.notification.presentation.NotificationController
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class NotificationFeatureFlagTest :
    StringSpec({
        @Configuration(proxyBeanMethods = false)
        class Dependencies {
            @Bean
            fun clubMemberReader() = mockk<ClubMemberReader>()

            @Bean
            fun createNotificationUseCase() = mockk<CreateNotificationUseCase>()

            @Bean
            fun manageNotificationTokenUseCase() = mockk<ManageNotificationTokenUseCase>()

            @Bean
            fun manageNotificationUseCase() = mockk<ManageNotificationUseCase>()

            @Bean
            fun getNotificationQueryService() = mockk<GetNotificationQueryService>()
        }

        val runner =
            ApplicationContextRunner()
                .withUserConfiguration(
                    Dependencies::class.java,
                    CreateNoticeNotificationEventListener::class.java,
                    NotificationController::class.java,
                )

        "weeth.notification.enabled가 없거나 false면 공지 알림 생성 리스너와 알림 API를 등록하지 않는다" {
            runner.run { context ->
                context.getBeansOfType(CreateNoticeNotificationEventListener::class.java).size shouldBe 0
                context.getBeansOfType(NotificationController::class.java).size shouldBe 0
            }
            runner.withPropertyValues("weeth.notification.enabled=false").run { context ->
                context.getBeansOfType(CreateNoticeNotificationEventListener::class.java).size shouldBe 0
                context.getBeansOfType(NotificationController::class.java).size shouldBe 0
            }
        }

        "weeth.notification.enabled=true면 공지 알림 생성 리스너와 알림 API를 등록한다" {
            runner.withPropertyValues("weeth.notification.enabled=true").run { context ->
                context.getBeansOfType(CreateNoticeNotificationEventListener::class.java).size shouldBe 1
                context.getBeansOfType(NotificationController::class.java).size shouldBe 1
            }
        }
    })
