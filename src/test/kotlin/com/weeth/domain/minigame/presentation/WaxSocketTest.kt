package com.weeth.domain.minigame.presentation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.weeth.prototype.wax.WaxPrototypeConfiguration
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class WaxSocketTest :
    StringSpec({
        /** 프로토타입 진입점을 임의 포트로 띄운다. args로 `weeth.wax.*` 한도를 바꿀 수 있다. */
        fun withServer(
            vararg args: String,
            block: (ServletWebServerApplicationContext, Int) -> Unit,
        ) {
            val context =
                SpringApplicationBuilder(WaxPrototypeConfiguration::class.java).run(
                    "--spring.config.name=wax-prototype",
                    "--server.port=0",
                    "--logging.level.root=WARN",
                    *args,
                ) as ServletWebServerApplicationContext
            try {
                block(context, context.webServer.port)
            } finally {
                context.close()
            }
        }

        "공개 id는 짧은 번호이고 좌표는 소수 둘째 자리까지, 기본값인 seat·pressing·emote 필드는 생략한다" {
            withServer { _, port ->
                val a = SocketProbe.connect(port)
                try {
                    a.send("""{"type":"join"}""")
                    val me = a.await { it["type"].asText() == "welcome" }["you"].asText()
                    (me.length <= 4) shouldBe true
                    val player = a.await { it["type"].asText() == "state" }["players"][0]
                    player["id"].asText() shouldBe me
                    player.has("emote") shouldBe false
                    player.has("seat") shouldBe false
                    player.has("pressing") shouldBe false
                    (player["x"].asText().substringAfter('.', "").length <= 2) shouldBe true
                    a.send("""{"type":"emote","kind":"wave"}""")
                    a.await { state -> state["players"]?.get(0)?.get("emote")?.asText() == "wave" }
                } finally {
                    a.socket.abort()
                }
            }
        }

        "같은 IP에서 한도를 넘는 연결은 too_many_connections로 거절한다" {
            withServer("--weeth.wax.max-connections-per-ip=2") { _, port ->
                val probes = (1..3).map { SocketProbe.connect(port) }
                try {
                    val rejected = probes[2].await { it["type"].asText() == "error" }
                    rejected["code"].asText() shouldBe "too_many_connections"
                    probes[0].send("""{"type":"join"}""")
                    probes[0].await { it["type"].asText() == "welcome" }
                } finally {
                    probes.forEach { it.socket.abort() }
                }
            }
        }

        "trust-forwarded-for가 켜져 있으면 X-Forwarded-For 첫 값으로 IP 한도를 센다" {
            withServer("--weeth.wax.max-connections-per-ip=1", "--weeth.wax.trust-forwarded-for=true") { _, port ->
                val same = mapOf("X-Forwarded-For" to "1.2.3.4, 10.0.0.1")
                val first = SocketProbe.connect(port, headers = same)
                // 프록시 뒤 실제 IP가 같은 두 번째 연결은 거절되고, 다른 IP는 받아들인다.
                val second = SocketProbe.connect(port, headers = mapOf("X-Forwarded-For" to "1.2.3.4"))
                val other = SocketProbe.connect(port, headers = mapOf("X-Forwarded-For" to "5.6.7.8, 10.0.0.1"))
                try {
                    second.await { it["type"].asText() == "error" }["code"].asText() shouldBe "too_many_connections"
                    other.send("""{"type":"join"}""")
                    other.await { it["type"].asText() == "welcome" }
                    first.send("""{"type":"join"}""")
                    first.await { it["type"].asText() == "welcome" }
                } finally {
                    listOf(first, second, other).forEach { it.socket.abort() }
                }
            }
        }

        "trust-forwarded-for가 꺼져 있으면 X-Forwarded-For를 무시하고 실제 접속 IP로 센다" {
            withServer("--weeth.wax.max-connections-per-ip=1") { _, port ->
                val first = SocketProbe.connect(port, headers = mapOf("X-Forwarded-For" to "1.2.3.4"))
                val spoofed = SocketProbe.connect(port, headers = mapOf("X-Forwarded-For" to "5.6.7.8"))
                try {
                    spoofed.await { it["type"].asText() == "error" }["code"].asText() shouldBe "too_many_connections"
                } finally {
                    first.socket.abort()
                    spoofed.socket.abort()
                }
            }
        }

        "닫힌 초대 링크는 같은 id로 다시 열고, 가득 찬 월드는 world_full, 형식이 틀린 id는 거절한다" {
            withServer("--weeth.wax.max-players-per-world=1") { _, port ->
                val a = SocketProbe.connect(port)
                val b = SocketProbe.connect(port)
                try {
                    a.send("""{"type":"join","worldId":"abcd1234"}""")
                    a.await { it["type"].asText() == "welcome" }["worldId"].asText() shouldBe "abcd1234"
                    b.send("""{"type":"join","worldId":"abcd1234"}""")
                    b.await { it["type"].asText() == "error" }["code"].asText() shouldBe "world_full"
                    b.send("""{"type":"join","worldId":"../evil"}""")
                    b.await { it["type"].asText() == "error" }["message"].asText() shouldBe "잘못된 초대 링크예요."
                    b.send("""{"type":"join"}""")
                    (b.await { it["type"].asText() == "welcome" }["worldId"].asText() == "abcd1234") shouldBe false
                } finally {
                    a.socket.abort()
                    b.socket.abort()
                }
            }
        }

        "입력이 없으면 idle 공지를 보내고 내보내며, heartbeat는 입력으로 치지 않는다" {
            withServer("--weeth.wax.idle-timeout-ms=600") { _, port ->
                val idle = SocketProbe.connect(port)
                val active = SocketProbe.connect(port)
                try {
                    idle.send("""{"type":"join"}""")
                    idle.await { it["type"].asText() == "welcome" }["idleTimeoutMs"].asLong() shouldBe 600
                    active.send("""{"type":"join"}""")
                    active.await { it["type"].asText() == "welcome" }
                    // idle 쪽은 heartbeat만, active 쪽은 화면 조작 신호를 보낸다.
                    repeat(6) {
                        // 내보내져 닫힌 뒤에는 전송이 실패하므로 무시한다.
                        runCatching { idle.send("""{"type":"heartbeat"}""") }
                        active.send("""{"type":"active"}""")
                        Thread.sleep(200)
                    }
                    idle.await { it["type"].asText() == "notice" }["code"].asText() shouldBe "idle"
                    active.messages.none { it.contains("\"code\":\"idle\"") } shouldBe true
                    active.await { state -> state["type"].asText() == "state" && state["players"].size() == 1 }
                } finally {
                    idle.socket.abort()
                    active.socket.abort()
                }
            }
        }

        "상태는 설정한 브로드캐스트 주기마다 보낸다" {
            withServer("--weeth.wax.broadcast-interval-ms=200") { _, port ->
                val a = SocketProbe.connect(port)
                try {
                    a.send("""{"type":"join"}""")
                    a.await { it["type"].asText() == "state" }
                    a.messages.clear()
                    Thread.sleep(1000)
                    val states = a.messages.count { it.contains("\"type\":\"state\"") }
                    (states in 3..7) shouldBe true
                } finally {
                    a.socket.abort()
                }
            }
        }

        "서버가 내려갈 때 restart 공지를 보낸다" {
            lateinit var a: SocketProbe
            // withServer가 끝나면서 컨텍스트를 닫는다. 그때 핸들러가 공지를 보낸다.
            withServer { _, port ->
                a = SocketProbe.connect(port)
                a.send("""{"type":"join"}""")
                a.await { it["type"].asText() == "welcome" }
            }
            a.await { it["type"].asText() == "notice" }["code"].asText() shouldBe "restart"
        }

        "두 브라우저가 같은 월드에 입장해 걸어서 강의실 좌석을 다투고 연결 해제 시 좌석을 해제한다" {
            val context =
                SpringApplicationBuilder(WaxPrototypeConfiguration::class.java).run(
                    "--spring.config.name=wax-prototype",
                    "--server.port=0",
                    "--logging.level.root=WARN",
                ) as ServletWebServerApplicationContext
            try {
                val port = context.webServer.port
                val a = SocketProbe.connect(port)
                val b = SocketProbe.connect(port)
                try {
                    a.send("""{"type":"join"}""")
                    val welcome = a.await { it["type"].asText() == "welcome" }
                    val world = welcome["worldId"].asText()
                    val me = welcome["you"].asText()
                    val room = welcome["layout"]["rooms"][0]
                    welcome["layout"]["rooms"].size() shouldBe 3
                    room["seats"].size() shouldBe 9
                    b.send("""{"type":"join","worldId":"$world"}""")
                    b.await { it["players"]?.size() == 2 }
                    val seat = room["seats"][0]
                    val doorX = room["doorX"].asDouble()
                    listOf(a, b).forEach { probe ->
                        val start = probe.await { it["type"].asText() == "state" }["you"]
                        probe.walk(start["x"].asDouble(), start["z"].asDouble(), doorX, 1.0)
                        probe.walk(doorX, 1.0, doorX, -1.0)
                        probe.walk(doorX, -1.0, seat["x"].asDouble() + 1.5, seat["z"].asDouble())
                    }
                    a.send("""{"type":"sit","room":1,"seat":0}""")
                    b.await { state ->
                        state["players"]?.any { it["id"].asText() == me && it.path("seat").asInt(-1) == 0 } == true
                    }
                    b.send("""{"type":"sit","room":1,"seat":0}""")
                    b.await { it["type"].asText() == "error" }["message"].asText().contains("이미") shouldBe true
                    a.send("""{"type":"emote","kind":"wave"}""")
                    b.await { state ->
                        state["players"]?.any { it["id"].asText() == me && it["emote"]?.asText() == "wave" } == true
                    }
                    a.send("""{"type":"emote","kind":"dance"}""")
                    a.await { it["type"].asText() == "error" }["message"].asText() shouldBe "없는 표현이에요."
                    a.socket.sendClose(WebSocket.NORMAL_CLOSURE, "bye").join()
                    b.await { it["players"]?.size() == 1 }
                    b.send("""{"type":"sit","room":1,"seat":0}""")
                    val seated = b.await { it["you"]?.get("seat")?.asInt(-1) == 0 }
                    seated["you"]["room"].asInt() shouldBe 1
                    seated["players"][0].has("noise") shouldBe false
                    seated["players"][0].has("crack") shouldBe false
                    seated["professors"].size() shouldBe 3
                } finally {
                    a.socket.abort()
                    b.socket.abort()
                }
            } finally {
                context.close()
            }
        }
    })

internal class SocketProbe : WebSocket.Listener {
    val messages = LinkedBlockingQueue<String>()
    lateinit var socket: WebSocket
    private val partial = StringBuilder()

    override fun onText(
        webSocket: WebSocket,
        data: CharSequence,
        last: Boolean,
    ): java.util.concurrent.CompletionStage<*>? {
        partial.append(data)
        if (last) {
            messages.add(partial.toString())
            partial.clear()
        }
        webSocket.request(1)
        return null
    }

    fun send(message: String) {
        socket.sendText(message, true).join()
    }

    /** 서버의 한 걸음 제한(1.5m)보다 짧게 끊어서 걷는다. */
    fun walk(
        fromX: Double,
        fromZ: Double,
        toX: Double,
        toZ: Double,
    ) {
        val steps = (kotlin.math.hypot(toX - fromX, toZ - fromZ) / 0.5).toInt() + 1
        (1..steps).forEach { step ->
            val t = step.toDouble() / steps
            send("""{"type":"move","x":${fromX + (toX - fromX) * t},"z":${fromZ + (toZ - fromZ) * t},"yaw":0}""")
        }
    }

    fun await(
        predicate: (com.fasterxml.jackson.databind.JsonNode) -> Boolean,
    ): com.fasterxml.jackson.databind.JsonNode {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (System.nanoTime() < deadline) {
            val message = messages.poll(1, TimeUnit.SECONDS) ?: continue
            val json = jacksonObjectMapper().readTree(message)
            if (predicate(json)) return json
        }
        error("상태 수신 시간 초과")
    }

    companion object {
        fun connect(
            port: Int,
            origin: String = "http://localhost:3103",
            headers: Map<String, String> = emptyMap(),
        ): SocketProbe {
            val probe = SocketProbe()
            val builder = HttpClient.newHttpClient().newWebSocketBuilder().header("Origin", origin)
            headers.forEach { (name, value) -> builder.header(name, value) }
            probe.socket = builder.buildAsync(URI("ws://127.0.0.1:$port/ws/wax"), probe).join()
            return probe
        }
    }
}
