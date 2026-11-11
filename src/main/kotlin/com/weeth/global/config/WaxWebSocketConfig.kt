package com.weeth.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.weeth.domain.minigame.presentation.WaxWebSocketHandler
import com.weeth.global.config.properties.WaxProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

/**
 * 왁뿌볼 실시간 엔드포인트. 상태가 메모리에만 있어 앱 인스턴스가 하나일 때만 켠다.
 * 앱이 재시작되면(blue/green 배포 포함) 월드가 초기화되고, 클라이언트는 restart 공지를 받고 재접속한다.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSocket
@ConditionalOnProperty(prefix = "weeth.wax", name = ["enabled"], havingValue = "true")
class WaxWebSocketConfig(
    objectMapper: ObjectMapper,
    private val properties: WaxProperties,
) : WebSocketConfigurer {
    private val handler = WaxWebSocketHandler(objectMapper, properties)

    @Bean(destroyMethod = "close")
    fun waxWebSocketHandler() = handler

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(handler, "/ws/wax").setAllowedOriginPatterns(*properties.allowedOrigins.toTypedArray())
    }
}
