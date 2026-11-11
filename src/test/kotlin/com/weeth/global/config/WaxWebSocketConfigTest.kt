package com.weeth.global.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.weeth.domain.minigame.presentation.WaxWebSocketHandler
import com.weeth.global.config.properties.WaxProperties
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.handler.AbstractHandlerMapping
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping

class WaxWebSocketConfigTest :
    StringSpec({
        @Configuration(proxyBeanMethods = false)
        @EnableConfigurationProperties(WaxProperties::class)
        class Base {
            @Bean
            fun objectMapper() = jacksonObjectMapper()
        }

        val runner =
            WebApplicationContextRunner()
                .withUserConfiguration(Base::class.java, WaxWebSocketConfig::class.java)

        "weeth.wax.enabled가 없거나 false면 엔드포인트를 등록하지 않는다" {
            runner.run { context -> context.getBeansOfType(WaxWebSocketHandler::class.java).size shouldBe 0 }
            runner.withPropertyValues("weeth.wax.enabled=false").run { context ->
                context.getBeansOfType(WaxWebSocketHandler::class.java).size shouldBe 0
            }
        }

        "weeth.wax.enabled=true면 /ws/wax에 핸들러를 등록하고 설정한 한도를 쓴다" {
            runner
                .withPropertyValues(
                    "weeth.wax.enabled=true",
                    "weeth.wax.max-players-per-world=12",
                    "weeth.wax.allowed-origins=https://*.v4.weeth.kr",
                ).run { context ->
                    context.getBeansOfType(WaxWebSocketHandler::class.java).size shouldBe 1
                    context.getBean(WaxProperties::class.java).maxPlayersPerWorld shouldBe 12
                    val mapped =
                        context
                            .getBeansOfType(AbstractHandlerMapping::class.java)
                            .values
                            .filterIsInstance<SimpleUrlHandlerMapping>()
                            .flatMap { it.urlMap.keys }
                    mapped.contains("/ws/wax") shouldBe true
                }
        }
    })
