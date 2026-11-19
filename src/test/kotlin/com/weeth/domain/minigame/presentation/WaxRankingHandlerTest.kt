package com.weeth.domain.minigame.presentation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.weeth.domain.minigame.domain.port.WaxRankingPort
import com.weeth.domain.minigame.domain.vo.WaxLayout
import com.weeth.domain.minigame.domain.vo.WaxNickname
import com.weeth.domain.minigame.domain.vo.WaxPlayerToken
import com.weeth.global.config.properties.WaxProperties
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.http.HttpHeaders
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.hypot

class WaxRankingHandlerTest :
    StringSpec({
        val mapper = jacksonObjectMapper()
        val ranking = mockk<WaxRankingPort>(relaxed = true)
        val token = WaxPlayerToken("3f2b8c1e-6a4d-4e2b-9c1f-7d8e9a0b1c2d")
        val room = WaxLayout.DEFAULT.room(1)

        beforeTest { clearMocks(ranking) }

        class Client(
            id: String,
        ) {
            val messages = CopyOnWriteArrayList<JsonNode>()
            val session =
                mockk<WebSocketSession>(relaxed = true).also { session ->
                    every { session.id } returns id
                    every { session.isOpen } returns true
                    every { session.handshakeHeaders } returns HttpHeaders()
                    every { session.remoteAddress } returns InetSocketAddress("127.0.0.1", 50000)
                    every { session.sendMessage(any()) } answers {
                        messages.add(mapper.readTree(firstArg<TextMessage>().payload))
                    }
                }

            fun bigCracks() =
                messages
                    .lastOrNull { it["type"]?.asText() == "state" }
                    ?.path("you")
                    ?.path("bigCracks")
                    ?.asInt(0) ?: 0

            fun await(predicate: (JsonNode) -> Boolean): JsonNode {
                val deadline = System.currentTimeMillis() + 3000
                while (System.currentTimeMillis() < deadline) {
                    messages.lastOrNull(predicate)?.let { return it }
                    Thread.sleep(20)
                }
                error("기다린 메시지가 오지 않았어요: ${messages.takeLast(3)}")
            }
        }

        fun withHandler(
            properties: WaxProperties = WaxProperties(),
            block: (WaxWebSocketHandler) -> Unit,
        ) {
            val handler = WaxWebSocketHandler(mapper, properties, ranking)
            try {
                block(handler)
            } finally {
                handler.close()
            }
        }

        fun WaxWebSocketHandler.send(
            client: Client,
            payload: String,
        ) = handleMessage(client.session, TextMessage(payload))

        /** 강의실 자리까지 0.5m씩 걸어가 앉은 뒤, 큰 깨짐이 날 때까지 누른다. 교수님이 처음 돌아보기(4.5초) 전에 끝난다. */
        fun WaxWebSocketHandler.breakWax(client: Client) {
            val seat = room.seats.first { WaxLayout.DEFAULT.variants[it.variant].hardness >= 46.0 }
            val start = client.await { it["type"]?.asText() == "state" }["you"]
            var x = start["x"].asDouble()
            var z = start["z"].asDouble()
            val path = listOf(room.doorX to 1.0, room.doorX to -1.0, seat.x + 1.5 to -1.0, seat.x + 1.5 to seat.z)
            path.forEach { (tx, tz) ->
                while (hypot(tx - x, tz - z) > 0.01) {
                    val length = hypot(tx - x, tz - z)
                    val step = minOf(0.5, length)
                    x += (tx - x) / length * step
                    z += (tz - z) / length * step
                    send(client, """{"type":"move","x":$x,"z":$z,"yaw":0}""")
                    // 초당 입력 한도(60)를 넘지 않게 천천히 보낸다.
                    Thread.sleep(25)
                }
            }
            send(client, """{"type":"sit","room":${room.id},"seat":${seat.index}}""")
            val deadline = System.currentTimeMillis() + 3500
            while (System.currentTimeMillis() < deadline) {
                send(client, """{"type":"press","active":true}""")
                Thread.sleep(100)
                if (client.bigCracks() > 0) return
            }
            error("큰 깨짐이 나지 않았어요")
        }

        "연결을 받아들이면 추천 닉네임을 보내고, 거절한 연결에는 보내지 않는다" {
            withHandler(WaxProperties(maxConnections = 1)) { handler ->
                val accepted = Client("1")
                val rejected = Client("2")
                handler.afterConnectionEstablished(accepted.session)
                handler.afterConnectionEstablished(rejected.session)
                accepted.messages.first()["type"].asText() shouldBe "hello"
                accepted.messages.first()["suggestion"].asText() shouldBeIn WaxNickname.CANDIDATES
                rejected.messages.none { it["type"].asText() == "hello" } shouldBe true
            }
        }

        "규칙을 벗어난 닉네임은 invalid_name으로 거절하고, 맞는 닉네임은 그 이름으로 입장한다" {
            withHandler { handler ->
                val client = Client("1")
                handler.afterConnectionEstablished(client.session)
                handler.send(client, """{"type":"join","name":"특수문자!"}""")
                client.await { it["type"]?.asText() == "error" }["code"].asText() shouldBe "invalid_name"
                handler.send(client, """{"type":"join","name":"왁뿌볼고수"}""")
                client.await { it["type"]?.asText() == "welcome" }
                client.await { state -> state["players"]?.any { it["name"].asText() == "왁뿌볼고수" } == true }
            }
        }

        "추천받은 이름을 그대로 보내면 공백이 있어도 입장한다" {
            withHandler { handler ->
                val client = Client("1")
                handler.afterConnectionEstablished(client.session)
                val suggestion = client.messages.first()["suggestion"].asText()
                handler.send(client, mapper.writeValueAsString(mapOf("type" to "join", "name" to suggestion)))
                client.await { state -> state["players"]?.any { it["name"].asText() == suggestion } == true }
            }
        }

        "입장하지 않은 연결은 설정한 대기 한도가 지나면 정리한다" {
            withHandler(WaxProperties(joinTimeoutMs = 200)) { handler ->
                val client = Client("1")
                handler.afterConnectionEstablished(client.session)
                Thread.sleep(150)
                verify(exactly = 0) { client.session.close(any()) }
                verify(timeout = 1000) { client.session.close(CloseStatus.NORMAL) }
            }
        }

        "토큰과 함께 왁스를 깨고 나가면 그 판 기록을 랭킹에 넘긴다" {
            withHandler { handler ->
                val client = Client("1")
                handler.afterConnectionEstablished(client.session)
                handler.send(client, """{"type":"join","name":"왁뿌볼고수","token":"${token.value}"}""")
                handler.breakWax(client)
                handler.afterConnectionClosed(client.session, CloseStatus.NORMAL)
                verify(timeout = 2000) {
                    ranking.record(match { it.tokenHash == token.hash() && it.name == "왁뿌볼고수" && it.brokenWaxes > 0 })
                }
            }
        }

        "서버가 내려갈 때도 남아 있던 플레이어의 기록을 넘긴다" {
            val handler = WaxWebSocketHandler(mapper, WaxProperties(), ranking)
            val client = Client("1")
            handler.afterConnectionEstablished(client.session)
            handler.send(client, """{"type":"join","token":"${token.value}"}""")
            handler.breakWax(client)
            handler.close()
            verify(exactly = 1) { ranking.record(match { it.tokenHash == token.hash() && it.brokenWaxes > 0 }) }
        }

        "토큰이 없거나 형식이 틀리거나 깬 왁스가 없으면 기록하지 않는다" {
            // 왁스를 깨는 판은 교수님 타이머가 겹치지 않게 각자 새 월드(새 핸들러)에서 한다.
            listOf(
                """{"type":"join"}""" to true,
                """{"type":"join","token":"bad"}""" to true,
                """{"type":"join","token":"${token.value}"}""" to false,
            ).forEach { (join, breaks) ->
                withHandler { handler ->
                    val client = Client("1")
                    handler.afterConnectionEstablished(client.session)
                    handler.send(client, join)
                    client.await { it["type"]?.asText() == "welcome" }
                    if (breaks) handler.breakWax(client)
                    handler.afterConnectionClosed(client.session, CloseStatus.NORMAL)
                }
            }
            verify(exactly = 0) { ranking.record(any()) }
        }
    })
