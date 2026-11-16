package com.weeth.global.config

import com.weeth.config.TestContainersConfig
import com.weeth.domain.minigame.presentation.SocketProbe
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.net.http.WebSocketHandshakeException
import java.util.concurrent.CompletionException

/** 운영 앱(Spring Security 포함)에 `/ws/wax`를 켰을 때 로그인 없이 핸드셰이크가 열리고, origin 밖은 막히는지. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["weeth.wax.enabled=true", "weeth.wax.allowed-origins=https://*.v4.weeth.kr"],
)
@ActiveProfiles("test")
@Import(TestContainersConfig::class)
class WaxSecurityIntegrationTest(
    @Value("\${local.server.port}") private val port: Int,
) : StringSpec({
        "토큰 없이 허용된 origin에서 접속하면 입장할 수 있다" {
            val probe = SocketProbe.connect(port, origin = "https://dev.v4.weeth.kr")
            try {
                probe.send("""{"type":"join"}""")
                probe.await { it["type"].asText() == "welcome" }["you"].asText().isNotEmpty() shouldBe true
            } finally {
                probe.socket.abort()
            }
        }

        "허용하지 않은 origin의 핸드셰이크는 403으로 거절한다" {
            val error = shouldThrow<CompletionException> { SocketProbe.connect(port, origin = "https://evil.example") }
            val handshake = error.cause.shouldBeInstanceOf<WebSocketHandshakeException>()
            handshake.response.statusCode() shouldBe 403
        }
    })
