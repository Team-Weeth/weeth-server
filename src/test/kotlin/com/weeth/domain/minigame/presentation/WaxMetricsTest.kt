package com.weeth.domain.minigame.presentation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.weeth.global.config.properties.WaxProperties
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.HttpHeaders
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.net.InetSocketAddress

class WaxMetricsTest :
    StringSpec({
        fun session(
            id: String,
            ip: String = "127.0.0.$id",
        ) = mockk<WebSocketSession>(relaxed = true).also { session ->
            every { session.id } returns id
            every { session.isOpen } returns true
            every { session.handshakeHeaders } returns HttpHeaders()
            every { session.remoteAddress } returns InetSocketAddress(ip, 50000)
        }

        fun withHandler(
            properties: WaxProperties = WaxProperties(),
            block: (WaxWebSocketHandler, SimpleMeterRegistry) -> Unit,
        ) {
            val registry = SimpleMeterRegistry()
            val handler = WaxWebSocketHandler(jacksonObjectMapper(), properties, registry)
            try {
                block(handler, registry)
            } finally {
                handler.close()
            }
        }

        fun SimpleMeterRegistry.gauge(name: String) = find(name).gauge()?.value()

        fun SimpleMeterRegistry.rejections(code: String) = find("wax.rejections").tag("code", code).counter()?.count()

        "게이지는 연결·입장·퇴장에 따라 열린 연결 수, 입장 인원, 월드 수를 보여준다" {
            withHandler { handler, registry ->
                val a = session("1")
                val b = session("2")
                handler.afterConnectionEstablished(a)
                handler.afterConnectionEstablished(b)
                registry.gauge("wax.connections") shouldBe 2.0
                registry.gauge("wax.players") shouldBe 0.0
                registry.gauge("wax.worlds") shouldBe 0.0

                listOf(a, b).forEach { handler.handleMessage(it, TextMessage("""{"type":"join"}""")) }
                registry.gauge("wax.players") shouldBe 2.0
                registry.gauge("wax.worlds") shouldBe 1.0

                handler.afterConnectionClosed(a, CloseStatus.NORMAL)
                registry.gauge("wax.connections") shouldBe 1.0
                registry.gauge("wax.players") shouldBe 1.0
                registry.gauge("wax.worlds") shouldBe 1.0
            }
        }

        "연결 단계 거절은 code 태그로 센다" {
            withHandler(WaxProperties(maxConnections = 1)) { handler, registry ->
                handler.afterConnectionEstablished(session("1"))
                handler.afterConnectionEstablished(session("2"))
                registry.rejections("server_full") shouldBe 1.0
                registry.gauge("wax.connections") shouldBe 1.0
            }
            withHandler(WaxProperties(maxConnectionsPerIp = 1)) { handler, registry ->
                handler.afterConnectionEstablished(session("1", ip = "10.0.0.1"))
                handler.afterConnectionEstablished(session("2", ip = "10.0.0.1"))
                registry.rejections("too_many_connections") shouldBe 1.0
            }
        }

        "입장 단계 거절도 센다" {
            withHandler(WaxProperties(maxWorlds = 1, maxPlayersPerWorld = 1)) { handler, registry ->
                val a = session("1")
                val b = session("2")
                handler.afterConnectionEstablished(a)
                handler.afterConnectionEstablished(b)
                handler.handleMessage(a, TextMessage("""{"type":"join"}"""))
                handler.handleMessage(b, TextMessage("""{"type":"join"}"""))
                registry.rejections("server_full") shouldBe 1.0
                registry.gauge("wax.players") shouldBe 1.0
            }
        }

        "거절이 아닌 잘못된 요청은 거절 수에 넣지 않는다" {
            withHandler { handler, registry ->
                val a = session("1")
                handler.afterConnectionEstablished(a)
                handler.handleMessage(a, TextMessage("""{"type":"move","x":0,"z":0,"yaw":0}"""))
                registry.find("wax.rejections").counters().size shouldBe 0
            }
        }

        "한도와 상관없는 닉네임 거절은 거절 수에 넣지 않는다" {
            withHandler { handler, registry ->
                val a = session("1")
                handler.afterConnectionEstablished(a)
                handler.handleMessage(a, TextMessage("""{"type":"join","name":"특수!"}"""))
                registry.find("wax.rejections").counters().size shouldBe 0
            }
        }
    })
