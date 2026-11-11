package com.weeth.prototype.wax

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.weeth.domain.minigame.presentation.WaxWebSocketHandler
import com.weeth.global.config.properties.WaxProperties
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

// @Configuration을 붙이지 않는다. 테스트 클래스패스에 이 클래스가 올라가면 @SpringBootTest의 컴포넌트 스캔(com.weeth)에
// 잡히고, 아래 exclude(DataSource·JPA·Security)가 앱 전체 자동 설정에 합쳐져 통합 테스트 컨텍스트가 깨진다.
// SpringApplicationBuilder의 기본 소스로 직접 등록하면 @Bean·@Enable* 는 그대로 동작한다.
@EnableWebSocket
@EnableAutoConfiguration(
    exclude = [
        DataSourceAutoConfiguration::class,
        HibernateJpaAutoConfiguration::class,
        FlywayAutoConfiguration::class,
        RedisAutoConfiguration::class,
        RedisRepositoriesAutoConfiguration::class,
        SecurityAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class,
        ManagementWebSecurityAutoConfiguration::class,
    ],
)
@EnableConfigurationProperties(WaxProperties::class)
class WaxPrototypeConfiguration(
    private val properties: WaxProperties,
) : WebSocketConfigurer {
    private val handler = WaxWebSocketHandler(jacksonObjectMapper(), properties)

    @Bean(destroyMethod = "close")
    fun waxHandler() = handler

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(handler, "/ws/wax").setAllowedOriginPatterns(*properties.allowedOrigins.toTypedArray())
    }
}

fun main(args: Array<String>) {
    SpringApplicationBuilder(WaxPrototypeConfiguration::class.java).run(*args)
}
