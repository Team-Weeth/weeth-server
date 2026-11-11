package com.weeth.domain.minigame.presentation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.weeth.domain.minigame.domain.entity.WaxWorld
import com.weeth.domain.minigame.domain.vo.WaxEmote
import com.weeth.domain.minigame.domain.vo.WaxPrivatePlayer
import com.weeth.domain.minigame.domain.vo.WaxPublicState
import com.weeth.global.config.properties.WaxProperties
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.IdentityHashMap
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** 클라이언트가 코드로 구분해 다르게 안내해야 하는 거절. */
class WaxRejection(
    val code: String,
    message: String,
) : IllegalArgumentException(message)

/** 전송 어댑터. 월드 변경은 lock 안에서, 직렬화와 느릴 수 있는 소켓 전송은 lock 밖에서 처리한다. */
class WaxWebSocketHandler(
    private val mapper: ObjectMapper,
    private val properties: WaxProperties = WaxProperties(),
) : TextWebSocketHandler(),
    AutoCloseable {
    private class Connection(
        val socket: WebSocketSession,
        val ip: String,
        val joinedAt: Long,
    ) {
        var world: WaxWorld? = null

        /** 월드 안에서 쓰는 짧은 공개 id. 세션 UUID(36자)를 매 스냅샷에 싣지 않으려고 따로 둔다. */
        var playerId = ""
        var messages = 0
        var windowAt = joinedAt
        var lastSeen = joinedAt

        /** 마지막 플레이 입력 시각. heartbeat로는 갱신하지 않는다. */
        var lastActiveAt = joinedAt
    }

    private val lock = Any()
    private val worlds = linkedMapOf<String, WaxWorld>()
    private val connections = linkedMapOf<String, Connection>()
    private var nextPlayerId = 0L
    private val scheduler =
        Executors.newSingleThreadScheduledExecutor { task ->
            Thread(task, "wax-tick").apply {
                isDaemon =
                    true
            }
        }
    private var previousTick = System.currentTimeMillis()
    private var previousBroadcast = 0L

    init {
        scheduler.scheduleAtFixedRate({ runCatching { tick() } }, TICK_MS, TICK_MS, TimeUnit.MILLISECONDS)
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        session.textMessageSizeLimit = 1024
        val ip = clientIp(session)
        val rejection =
            synchronized(lock) {
                when {
                    connections.size >= properties.maxConnections -> {
                        WaxRejection("server_full", "지금은 교실이 모두 가득 찼어요. 잠시 후 다시 와주세요.")
                    }

                    connections.values.count { it.ip == ip } >= properties.maxConnectionsPerIp -> {
                        WaxRejection("too_many_connections", "한 기기에서 너무 많이 접속했어요. 다른 창을 닫고 다시 와주세요.")
                    }

                    else -> {
                        connections[session.id] =
                            Connection(
                                ConcurrentWebSocketSessionDecorator(session, 1000, 128 * 1024),
                                ip,
                                System.currentTimeMillis(),
                            )
                        null
                    }
                }
            }
        if (rejection != null) {
            runCatching { session.sendMessage(TextMessage(mapper.writeValueAsString(errorMessage(rejection)))) }
            session.close(CloseStatus.SERVICE_OVERLOAD)
        }
    }

    private fun clientIp(session: WebSocketSession): String {
        val forwarded =
            if (properties.trustForwardedFor) {
                session.handshakeHeaders
                    .getFirst("X-Forwarded-For")
                    ?.substringBefore(',')
                    ?.trim()
            } else {
                null
            }
        return forwarded?.takeIf { it.isNotEmpty() } ?: session.remoteAddress?.address?.hostAddress ?: "unknown"
    }

    override fun handleTextMessage(
        session: WebSocketSession,
        message: TextMessage,
    ) {
        if (message.payloadLength > 1024) {
            session.close(CloseStatus.TOO_BIG_TO_PROCESS)
            return
        }
        val now = System.currentTimeMillis()
        try {
            val json = mapper.readTree(message.payload)
            val response =
                synchronized(lock) {
                    val connection = connections[session.id] ?: return
                    if (now - connection.windowAt >= 1000) {
                        connection.messages = 0
                        connection.windowAt = now
                    }
                    require(++connection.messages <= MAX_MESSAGES_PER_SECOND) { "입력이 너무 빨라요." }
                    connection.lastSeen = now
                    if (json.path("type").asText() != "heartbeat") connection.lastActiveAt = now
                    process(connection, json, now)
                }
            if (response != null) send(session.id, mapper.writeValueAsString(response))
        } catch (error: Exception) {
            send(session.id, mapper.writeValueAsString(errorMessage(error)))
        }
    }

    private fun errorMessage(error: Exception): Map<String, String> {
        val message = error.message?.take(120) ?: "잘못된 요청이에요."
        return if (error is WaxRejection) {
            mapOf("type" to "error", "code" to error.code, "message" to message)
        } else {
            mapOf("type" to "error", "message" to message)
        }
    }

    private fun process(
        connection: Connection,
        json: JsonNode,
        now: Long,
    ): Any? {
        require(json.isObject) { "잘못된 요청이에요." }
        val type = json.path("type").asText()
        if (type == "join") {
            require(connection.world == null) { "이미 입장했어요." }
            val world = if (json.has("worldId")) invitedWorld(json["worldId"]) else openWorld()
            val playerId = (++nextPlayerId).toString(36)
            val welcome = world.join(playerId)
            connection.world = world
            connection.playerId = playerId
            return welcome.copy(idleTimeoutMs = properties.idleTimeoutMs)
        }
        val world = requireNotNull(connection.world) { "먼저 입장해주세요." }
        val id = connection.playerId
        when (type) {
            "move" -> {
                require(json.path("x").isNumber && json.path("z").isNumber && json.path("yaw").isNumber) { "잘못된 좌표예요." }
                world.move(id, json["x"].asDouble(), json["z"].asDouble(), json["yaw"].asDouble())
            }

            "sit" -> {
                world.sit(id, integer(json, "room"), integer(json, "seat"))
            }

            "stand" -> {
                world.stand(id)
            }

            "press" -> {
                require(json.path("active").isBoolean) { "잘못된 입력이에요." }
                world.press(id, json["active"].asBoolean(), now)
            }

            "emote" -> {
                require(json.path("kind").isTextual) { "잘못된 입력이에요." }
                world.emote(id, WaxEmote.from(json["kind"].asText()), now)
            }

            // heartbeat는 연결 유지만, active는 화면 조작(둘러보기 등 서버로 가는 명령이 없는 입력)이 있었다는 신호다.
            "heartbeat", "active" -> {
                Unit
            }

            else -> {
                throw IllegalArgumentException("알 수 없는 동작이에요.")
            }
        }
        return null
    }

    /**
     * 초대 링크의 월드. 모두 나가서 닫힌 월드면 같은 id로 다시 연다.
     * 그래야 링크를 늦게 누른 친구들도 한 교실에 모인다.
     */
    private fun invitedWorld(node: JsonNode): WaxWorld {
        require(node.isTextual && WORLD_ID.matches(node.asText())) { "잘못된 초대 링크예요." }
        val world = worlds[node.asText()] ?: newWorld(node.asText())
        if (world.isFull) throw WaxRejection("world_full", "친구가 있는 교실이 가득 찼어요.")
        return world
    }

    /** 자리가 남은 월드부터 채운다. 사람이 모여 있어야 재미있는 게임이라 새 월드는 마지막에 연다. */
    private fun openWorld() = worlds.values.firstOrNull { !it.isFull } ?: newWorld(freshWorldId())

    /** UUID 앞 8자는 드물게 겹칠 수 있다. 겹치면 기존 월드를 덮어써 그 교실 사람들이 끊기므로 비어 있는 id를 고른다. */
    private fun freshWorldId(): String {
        while (true) {
            val id = UUID.randomUUID().toString().take(8)
            if (id !in worlds) return id
        }
    }

    private fun newWorld(id: String): WaxWorld {
        if (worlds.size >= properties.maxWorlds) throw WaxRejection("server_full", "지금은 교실이 모두 가득 찼어요.")
        return WaxWorld(id, capacity = properties.maxPlayersPerWorld).also { worlds[it.id] = it }
    }

    private fun integer(
        json: JsonNode,
        key: String,
    ): Int {
        require(json.path(key).isIntegralNumber && json.path(key).canConvertToInt()) { "잘못된 입력이에요." }
        return json[key].asInt()
    }

    private fun tick() {
        val now = System.currentTimeMillis()
        val expired = mutableListOf<WebSocketSession>()
        val idle = mutableListOf<WebSocketSession>()
        val frame =
            synchronized(lock) {
                val delta = (now - previousTick) / 1000.0
                previousTick = now
                worlds.values.forEach { it.tick(now, delta) }
                connections.values.forEach { connection ->
                    if (now - connection.lastSeen > 30000 ||
                        (connection.world == null && now - connection.joinedAt > 10000)
                    ) {
                        expired.add(connection.socket)
                    } else if (connection.world != null && now - connection.lastActiveAt > properties.idleTimeoutMs) {
                        idle.add(connection.socket)
                    }
                }
                worlds.entries.removeIf { it.value.size == 0 }
                // tick 타이머가 몇 ms 늦게 돌아도 한 번씩 건너뛰지 않도록 tick 절반만큼 여유를 둔다.
                if (now - previousBroadcast + TICK_MS / 2 < properties.broadcastIntervalMs) {
                    null
                } else {
                    previousBroadcast = now
                    val shared = worlds.values.associateWith { it.publicState(now) }
                    connections.values.mapNotNull { connection ->
                        val world = connection.world ?: return@mapNotNull null
                        Triple(connection.socket.id, shared.getValue(world), world.privateState(connection.playerId))
                    }
                }
            }
        frame?.let { broadcast(it) }
        expired.forEach { socket ->
            disconnect(socket.id)
            runCatching { socket.close(CloseStatus.NORMAL) }
        }
        // 클라이언트는 idle 공지를 받으면 자동 재접속하지 않고 다시 입장 버튼을 보여준다.
        idle.forEach { socket ->
            runCatching { socket.sendMessage(TextMessage(IDLE_NOTICE)) }
            disconnect(socket.id)
            runCatching { socket.close(CloseStatus.NORMAL) }
        }
    }

    /** 공개 부분은 월드당 한 번만 직렬화하고 연결마다 본인 상태(`you`)만 붙인다. */
    private fun broadcast(frame: List<Triple<String, WaxPublicState, WaxPrivatePlayer>>) {
        // 같은 월드의 연결은 같은 상태 객체를 공유하므로 내용 비교 없이 객체 동일성으로 묶는다.
        val shared = IdentityHashMap<WaxPublicState, String>()
        frame.forEach { (id, state, you) ->
            val public = shared.getOrPut(state) { mapper.writeValueAsString(state).dropLast(1) }
            send(id, """$public,"type":"state","you":${mapper.writeValueAsString(you)}}""")
        }
    }

    private fun send(
        id: String,
        payload: String,
    ) {
        val socket = synchronized(lock) { connections[id]?.socket } ?: return
        try {
            if (socket.isOpen) socket.sendMessage(TextMessage(payload))
        } catch (
            _: Exception,
        ) {
            disconnect(id)
            runCatching { socket.close(CloseStatus.SERVER_ERROR) }
        }
    }

    override fun afterConnectionClosed(
        session: WebSocketSession,
        status: CloseStatus,
    ) {
        disconnect(session.id)
    }

    override fun handleTransportError(
        session: WebSocketSession,
        exception: Throwable,
    ) {
        disconnect(session.id)
        runCatching { session.close(CloseStatus.SERVER_ERROR) }
    }

    private fun disconnect(id: String) {
        synchronized(lock) {
            val connection = connections.remove(id) ?: return
            connection.world?.leave(connection.playerId)
        }
    }

    /**
     * 컨텍스트 종료가 시작될 때 먼저 닫는다. 빈 소멸(destroyMethod) 시점에는 웹 서버가 이미 멈춰
     * WebSocket 연결이 끊긴 뒤라 공지를 보낼 수 없다.
     */
    @EventListener(ContextClosedEvent::class)
    fun onContextClosed() = close()

    /** 배포·재시작으로 내려갈 때 클라이언트가 재접속하도록 알린 뒤 닫는다. 두 번 불려도 안전하다. */
    override fun close() {
        // 인터럽트(shutdownNow)하면 전송 중이던 브로드캐스트가 실패해 연결이 공지 전에 닫힌다.
        // 진행 중인 tick이 끝나길 기다린 뒤 보내야 동시 전송 버퍼에 걸리지 않고 공지가 바로 나간다.
        scheduler.shutdown()
        if (!scheduler.awaitTermination(SHUTDOWN_WAIT_MS, TimeUnit.MILLISECONDS)) scheduler.shutdownNow()
        val sockets =
            synchronized(lock) {
                connections.values.map { it.socket }.also {
                    connections.clear()
                    worlds.clear()
                }
            }
        sockets.forEach { socket ->
            runCatching { socket.sendMessage(TextMessage(RESTART_NOTICE)) }
            runCatching { socket.close(CloseStatus.GOING_AWAY) }
        }
    }

    companion object {
        private const val TICK_MS = 50L
        private const val SHUTDOWN_WAIT_MS = 2000L
        private const val MAX_MESSAGES_PER_SECOND = 60
        private val WORLD_ID = Regex("[a-z0-9]{8}")
        private const val IDLE_NOTICE =
            """{"type":"notice","code":"idle","message":"오래 움직이지 않아 교실에서 나왔어요."}"""
        private const val RESTART_NOTICE = """{"type":"notice","code":"restart","message":"교실을 정비하고 있어요. 곧 다시 연결해요."}"""
    }
}
