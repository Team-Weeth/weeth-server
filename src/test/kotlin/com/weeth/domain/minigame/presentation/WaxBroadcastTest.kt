package com.weeth.domain.minigame.presentation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.weeth.domain.minigame.domain.vo.WaxPublicState
import com.weeth.global.config.properties.WaxProperties
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.HttpHeaders
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

class WaxBroadcastTest :
    StringSpec({
        "같은 월드의 연결이 여럿이어도 공개 상태는 브로드캐스트마다 한 번만 직렬화한다" {
            val publicSerializations = AtomicInteger()
            val mapper =
                object : com.fasterxml.jackson.databind.ObjectMapper(jacksonObjectMapper()) {
                    override fun writeValueAsString(value: Any?): String {
                        if (value is WaxPublicState) publicSerializations.incrementAndGet()
                        return super.writeValueAsString(value)
                    }
                }
            val stateSends = AtomicInteger()

            fun session(id: String) =
                mockk<WebSocketSession>(relaxed = true).also { session ->
                    every { session.id } returns id
                    every { session.isOpen } returns true
                    every { session.handshakeHeaders } returns HttpHeaders()
                    every { session.remoteAddress } returns InetSocketAddress("127.0.0.1", 50000)
                    every { session.sendMessage(any()) } answers {
                        if ((firstArg<TextMessage>().payload).contains(
                                "\"type\":\"state\"",
                            )
                        ) {
                            stateSends.incrementAndGet()
                        }
                    }
                }

            val handler = WaxWebSocketHandler(mapper, WaxProperties(broadcastIntervalMs = 50))
            try {
                val sessions = (1..3).map { session("s$it") }
                sessions.forEach { session ->
                    handler.afterConnectionEstablished(session)
                    handler.handleMessage(session, TextMessage("""{"type":"join"}"""))
                }
                Thread.sleep(600)
            } finally {
                handler.close()
            }
            val serialized = publicSerializations.get()
            serialized shouldBeGreaterThan 2
            // 연결 3개가 모두 같은 월드라 전송 수는 직렬화 수의 3배다. 연결마다 직렬화했다면 두 값이 같았을 것이다.
            // close()가 마지막 브로드캐스트 도중에 연결을 비우면 그 회차는 일부만 전송될 수 있어 2건까지 허용한다.
            (stateSends.get() in (serialized * 3 - 2)..(serialized * 3)) shouldBe true
        }
    })
