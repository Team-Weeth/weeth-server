package com.weeth.domain.minigame.domain.entity

import com.weeth.domain.minigame.domain.vo.WaxEjection
import com.weeth.domain.minigame.domain.vo.WaxEmote
import com.weeth.domain.minigame.domain.vo.WaxLayout
import com.weeth.domain.minigame.domain.vo.WaxPlayerStatus
import com.weeth.domain.minigame.domain.vo.WaxPoint
import com.weeth.domain.minigame.domain.vo.WaxPrivatePlayer
import com.weeth.domain.minigame.domain.vo.WaxProfessorMode
import com.weeth.domain.minigame.domain.vo.WaxProfessorState
import com.weeth.domain.minigame.domain.vo.WaxPublicPlayer
import com.weeth.domain.minigame.domain.vo.WaxPublicState
import com.weeth.domain.minigame.domain.vo.WaxRoom
import com.weeth.domain.minigame.domain.vo.WaxSeat
import com.weeth.domain.minigame.domain.vo.WaxSnapshot
import com.weeth.domain.minigame.domain.vo.WaxWelcome
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToLong
import kotlin.random.Random

/** 메모리 월드의 상태와 규칙. 호출자는 월드의 모든 접근을 같은 실행 경계에서 직렬화한다. */
class WaxWorld(
    val id: String,
    private val random: Random = Random.Default,
    val layout: WaxLayout = WaxLayout.DEFAULT,
    val capacity: Int = MAX_PLAYERS,
) {
    private class Player(
        val id: String,
        val name: String,
        val character: String,
        var x: Double,
        var z: Double,
    ) {
        var yaw = 0.0
        var seat: WaxSeat? = null
        var seatRoom = 0
        var status = WaxPlayerStatus.WALKING
        var pressing = false
        var lastPressAt = 0L
        var releasedAt = 0L
        var noise = 0.0
        var crack = 0.0
        var stage = 0
        var layer = 0
        var smallCracks = 0
        var bigCracks = 0
        var caughts = 0
        var emote: WaxEmote? = null
        var emoteAt = 0L
    }

    private class Professor(
        val room: WaxRoom,
    ) {
        var mode = WaxProfessorMode.WRITING
        var x = room.professorHome.x
        var z = room.professorHome.z
        var yaw = PI
        var until = UNSCHEDULED
        var targetId: String? = null
        val queue = ArrayDeque<String>()
        val path = ArrayDeque<WaxPoint>()

        val busy get() = mode == WaxProfessorMode.CHASING || mode == WaxProfessorMode.WHACKING

        fun state() = WaxProfessorState(room.id, mode, round2(x), round2(z), round2(yaw), targetId)
    }

    private val players = linkedMapOf<String, Player>()
    private val professors = layout.rooms.associate { it.id to Professor(it) }
    private val ejections = mutableListOf<WaxEjection>()
    val size: Int get() = players.size
    val isFull: Boolean get() = players.size >= capacity

    fun join(id: String): WaxWelcome {
        require(id !in players) { "이미 입장했어요." }
        require(!isFull) { "교실이 가득 찼어요. 잠시 후 다시 와주세요." }
        // 캐릭터 5종과 이름을 입장할 때마다 무작위로 고른다.
        val name = pickNickname()
        val character = WaxLayout.STUDENT_CHARACTERS[random.nextInt(WaxLayout.STUDENT_CHARACTERS.size)]
        players[id] = Player(id, name, character, random.nextDouble(-3.0, 3.0), 4.5)
        return WaxWelcome(this.id, id, layout)
    }

    /** 같은 교실에서 이름이 겹치지 않게 남은 후보에서 고른다. 후보를 다 쓰면 겹침을 허용한다. */
    private fun pickNickname(): String {
        val taken = players.values.mapTo(HashSet()) { it.name }
        val available = NICKNAMES.filter { it !in taken }.ifEmpty { NICKNAMES }
        return available[random.nextInt(available.size)]
    }

    fun leave(id: String) {
        players.remove(id) ?: return
        professors.values.forEach { professor ->
            professor.queue.remove(id)
            if (professor.targetId == id) startReturn(professor)
        }
    }

    fun move(
        id: String,
        x: Double,
        z: Double,
        yaw: Double,
    ) {
        require(x.isFinite() && z.isFinite() && yaw.isFinite()) { "이동할 수 없는 위치예요." }
        val player = player(id)
        if (player.status != WaxPlayerStatus.WALKING) return
        player.yaw = yaw
        // 벽을 통과하거나 순간이동하는 좌표는 반영하지 않는다. 클라이언트는 다음 스냅샷으로 위치를 보정한다.
        if (!layout.canStep(player.x, player.z, x, z)) return
        player.x = x
        player.z = z
    }

    fun sit(
        id: String,
        room: Int,
        seatIndex: Int,
    ) {
        val player = player(id)
        require(player.status == WaxPlayerStatus.WALKING) { "지금은 앉을 수 없어요." }
        val seat = requireNotNull(layout.room(room).seats.getOrNull(seatIndex)) { "없는 좌석이에요." }
        require(layout.roomAt(player.x, player.z) == room) { "강의실에 먼저 들어가주세요." }
        require(hypot(player.x - seat.x, player.z - seat.z) <= WaxLayout.SIT_DISTANCE) { "의자에 더 가까이 가주세요." }
        require(players.values.none { it.seatRoom == room && it.seat?.index == seatIndex }) { "이미 친구가 앉아 있어요." }
        player.seat = seat
        player.seatRoom = room
        player.status = WaxPlayerStatus.SEATED
        player.x = seat.x
        player.z = seat.z
        player.yaw = PI
        player.pressing = false
        player.crack = 0.0
        player.stage = 0
    }

    fun stand(id: String) {
        val player = player(id)
        val seat = player.seat ?: return
        require(player.status == WaxPlayerStatus.SEATED) { "지금은 일어날 수 없어요." }
        clearSeat(player)
        player.x = seat.x + AISLE_OFFSET
        player.z = seat.z
    }

    fun press(
        id: String,
        active: Boolean,
        now: Long,
    ) {
        val player = player(id)
        if (!active) {
            if (player.pressing) player.releasedAt = now
            player.pressing = false
            return
        }
        require(player.status == WaxPlayerStatus.SEATED) { "빈 의자에 먼저 앉아주세요." }
        player.pressing = true
        player.lastPressAt = now
    }

    /** 표현은 상태와 관계없이 보낼 수 있다. 연타는 쿨다운 동안 조용히 무시한다. */
    fun emote(
        id: String,
        emote: WaxEmote,
        now: Long,
    ) {
        val player = player(id)
        if (player.emote != null && now - player.emoteAt < EMOTE_COOLDOWN_MS) return
        player.emote = emote
        player.emoteAt = now
    }

    fun tick(
        now: Long,
        deltaSeconds: Double,
    ) {
        val dt = deltaSeconds.coerceIn(0.0, 0.25)
        professors.values.forEach { advance(it, now, dt) }
        players.values.forEach { player ->
            if (player.pressing && now - player.lastPressAt > PRESS_TIMEOUT_MS) {
                player.pressing = false
                player.releasedAt = now
            }
            if (player.status != WaxPlayerStatus.SEATED) return@forEach
            val professor = professors.getValue(player.seatRoom)
            val alert = ALERTNESS.getValue(professor.mode)
            if (player.pressing) {
                pressTick(player, alert, dt)
            } else if (now - player.releasedAt > NOISE_DECAY_DELAY_MS) {
                player.noise = (player.noise - NOISE_DECAY * dt).coerceAtLeast(0.0)
            }
            if (player.noise >= NOISE_LIMIT) spot(professor, player)
        }
        ejections.removeAll { now - it.at > EJECTION_VISIBLE_MS }
    }

    fun snapshot(
        id: String,
        now: Long,
    ): WaxSnapshot {
        val shared = publicState(now)
        return WaxSnapshot(shared.now, shared.players, shared.professors, privateState(id), shared.ejections)
    }

    /** 모두에게 같은 부분. 좌표는 전송량을 줄이려 소수 둘째 자리(1cm)로 반올림한다. */
    fun publicState(now: Long) =
        WaxPublicState(
            now = now,
            players =
                players.values.map {
                    WaxPublicPlayer(
                        it.id,
                        it.name,
                        it.character,
                        round2(it.x),
                        round2(it.z),
                        round2(it.yaw),
                        roomOf(it),
                        it.seat?.index,
                        it.status,
                        it.pressing,
                        if (now - it.emoteAt < EMOTE_VISIBLE_MS) it.emote else null,
                    )
                },
            professors = professors.values.map { it.state() },
            ejections = ejections.toList(),
        )

    fun privateState(id: String): WaxPrivatePlayer {
        val me = player(id)
        return WaxPrivatePlayer(
            id = me.id,
            room = roomOf(me),
            seat = me.seat?.index,
            x = round2(me.x),
            z = round2(me.z),
            status = me.status,
            pressing = me.pressing,
            noise = round2(me.noise),
            crack = round2(me.crack),
            layer = me.layer,
            smallCracks = me.smallCracks,
            bigCracks = me.bigCracks,
            caughts = me.caughts,
        )
    }

    private fun pressTick(
        player: Player,
        alert: Double,
        dt: Double,
    ) {
        val variant = layout.variants[requireNotNull(player.seat).variant]
        player.crack += variant.hardness * dt
        player.noise += SQUISH_NOISE * alert * dt
        val nextSmall = SMALL_CRACK_AT.getOrNull(player.stage)
        if (nextSmall != null && player.crack >= nextSmall) {
            player.stage++
            player.smallCracks++
            player.noise += SMALL_CRACK_NOISE * variant.loudness * alert
        }
        if (player.crack >= 100) {
            player.crack = 0.0
            player.stage = 0
            player.layer++
            player.bigCracks++
            player.noise += BIG_CRACK_NOISE * variant.loudness * alert
        }
    }

    private fun spot(
        professor: Professor,
        player: Player,
    ) {
        player.status = WaxPlayerStatus.SPOTTED
        player.pressing = false
        player.noise = NOISE_LIMIT
        if (player.id !in professor.queue) professor.queue.addLast(player.id)
    }

    /** 교수님 상태 기계: 판서 → 돌아보기 → 지켜보기를 반복하고, 들킨 학생이 있으면 통로로 다가가 때린 뒤 돌아간다. */
    private fun advance(
        professor: Professor,
        now: Long,
        dt: Double,
    ) {
        if (!professor.busy && professor.queue.isNotEmpty()) startChase(professor)
        when (professor.mode) {
            WaxProfessorMode.CHASING -> {
                val target = professor.targetId?.let { players[it] }
                val seat = target?.seat
                if (target == null || seat == null) {
                    startReturn(professor)
                    return
                }
                if (followPath(professor, CHASE_SPEED, dt)) {
                    professor.yaw = atan2(seat.x - professor.x, seat.z - professor.z)
                    professor.mode = WaxProfessorMode.WHACKING
                    professor.until = now + WHACK_MS
                    target.status = WaxPlayerStatus.WHACKED
                }
            }

            WaxProfessorMode.WHACKING -> {
                if (now < professor.until) return
                professor.targetId?.let { players[it] }?.let { eject(it, professor.room, now) }
                startReturn(professor)
            }

            WaxProfessorMode.RETURNING -> {
                if (followPath(professor, RETURN_SPEED, dt)) {
                    professor.yaw = PI
                    professor.mode = WaxProfessorMode.WRITING
                    professor.until = now + random.nextLong(WRITING_MS.first, WRITING_MS.last)
                }
            }

            WaxProfessorMode.WRITING -> {
                if (professor.until ==
                    UNSCHEDULED
                ) {
                    professor.until = now + random.nextLong(WRITING_MS.first, WRITING_MS.last)
                }
                if (now >= professor.until) {
                    professor.mode = WaxProfessorMode.TURNING
                    professor.until = now + TURNING_MS
                }
            }

            WaxProfessorMode.TURNING -> {
                professor.yaw = 0.0
                if (now >= professor.until) {
                    professor.mode = WaxProfessorMode.WATCHING
                    professor.until = now + random.nextLong(WATCHING_MS.first, WATCHING_MS.last)
                }
            }

            WaxProfessorMode.WATCHING -> {
                if (now >= professor.until) {
                    professor.yaw = PI
                    professor.mode = WaxProfessorMode.WRITING
                    professor.until = now + random.nextLong(WRITING_MS.first, WRITING_MS.last)
                }
            }
        }
    }

    /** 책상을 가로지르지 않도록 교단 앞 → 학생 옆 통로 순서로 걷는다. */
    private fun startChase(professor: Professor) {
        val target = players[professor.queue.removeFirst()]
        val seat = target?.seat ?: return
        val aisleX = seat.x + AISLE_OFFSET
        professor.targetId = target.id
        professor.mode = WaxProfessorMode.CHASING
        professor.path.clear()
        if (professor.z > FRONT_Z + 0.05) professor.path.addLast(WaxPoint(professor.x, FRONT_Z))
        professor.path.addLast(WaxPoint(aisleX, FRONT_Z))
        professor.path.addLast(WaxPoint(aisleX, seat.z))
    }

    private fun startReturn(professor: Professor) {
        val home = professor.room.professorHome
        professor.targetId = null
        professor.mode = WaxProfessorMode.RETURNING
        professor.path.clear()
        if (professor.z > FRONT_Z + 0.05) professor.path.addLast(WaxPoint(professor.x, FRONT_Z))
        professor.path.addLast(home)
    }

    /** 경로를 따라 걷는다. 마지막 지점에 도착하면 true. */
    private fun followPath(
        professor: Professor,
        speed: Double,
        dt: Double,
    ): Boolean {
        var budget = speed * dt
        while (budget > 0) {
            val next = professor.path.firstOrNull() ?: return true
            val distance = hypot(next.x - professor.x, next.z - professor.z)
            if (distance <= budget) {
                professor.x = next.x
                professor.z = next.z
                professor.path.removeFirst()
                budget -= distance
            } else {
                professor.yaw = atan2(next.x - professor.x, next.z - professor.z)
                professor.x += (next.x - professor.x) / distance * budget
                professor.z += (next.z - professor.z) / distance * budget
                budget = 0.0
            }
        }
        return professor.path.isEmpty()
    }

    private fun eject(
        player: Player,
        room: WaxRoom,
        now: Long,
    ) {
        clearSeat(player)
        val exit = room.ejectPoint
        player.x = exit.x
        player.z = exit.z
        player.yaw = 0.0
        player.noise = 0.0
        player.caughts++
        ejections.add(WaxEjection(player.id, player.name, room.id, now))
    }

    private fun clearSeat(player: Player) {
        player.seat = null
        player.seatRoom = 0
        player.status = WaxPlayerStatus.WALKING
        player.pressing = false
        player.crack = 0.0
        player.stage = 0
    }

    private fun roomOf(player: Player) = if (player.seat != null) player.seatRoom else layout.roomAt(player.x, player.z)

    private fun player(id: String) = requireNotNull(players[id]) { "접속이 끝났어요. 다시 입장해주세요." }

    companion object {
        const val MAX_PLAYERS = 24
        const val NOISE_LIMIT = 100.0
        private const val AISLE_OFFSET = 1.5
        private const val FRONT_Z = -7.0
        private const val UNSCHEDULED = Long.MIN_VALUE
        private const val PRESS_TIMEOUT_MS = 1200L
        private const val NOISE_DECAY_DELAY_MS = 350L
        private const val NOISE_DECAY = 22.0
        private const val SQUISH_NOISE = 7.0
        private const val SMALL_CRACK_NOISE = 11.0
        private const val BIG_CRACK_NOISE = 36.0
        private val SMALL_CRACK_AT = listOf(34.0, 67.0)
        private const val CHASE_SPEED = 5.5
        private const val RETURN_SPEED = 2.6
        private const val WHACK_MS = 700L
        private const val TURNING_MS = 700L
        private val WRITING_MS = 4500L..8000L
        private val WATCHING_MS = 2200L..3800L
        private const val EJECTION_VISIBLE_MS = 3500L
        const val EMOTE_VISIBLE_MS = 2500L
        const val EMOTE_COOLDOWN_MS = 800L

        /** 교수님 상태별 소음 배율. 돌아보는 중일수록 같은 소리도 크게 들킨다. */
        private val ALERTNESS =
            mapOf(
                WaxProfessorMode.WRITING to 1.0,
                WaxProfessorMode.TURNING to 1.6,
                WaxProfessorMode.WATCHING to 2.4,
                WaxProfessorMode.CHASING to 1.6,
                WaxProfessorMode.WHACKING to 1.6,
                WaxProfessorMode.RETURNING to 1.6,
            )

        /** 학교·강의실 콘셉트의 닉네임 후보. */
        internal val NICKNAMES =
            listOf(
                "졸고 있는 새내기",
                "당당한 지각생",
                "수상한 복학생",
                "용감한 청강생",
                "배고픈 대학원생",
                "억울한 재수강생",
                "딴짓하는 장학생",
                "조용한 수강생",
                "뻔뻔한 지각생",
                "은밀한 지각생",
                "대담한 새내기",
                "들킬 뻔한 복학생",
                "긴장한 장학생",
                "수상한 뒷자리생",
                "눈치 빠른 신입생",
                "몰래 노는 과대표",
                "겁 없는 재수강생",
                "학점 잃은 새내기",
                "출석만 하는 복학생",
                "과제 밀린 복학생",
                "종강 찾는 대학생",
                "학식 먹는 지각생",
                "출튀하는 복학생",
                "밤샌 대학원생",
                "학점 줍는 재수강생",
                "은밀한 복학생",
                "수상한 새내기",
                "대담한 지각생",
            )

        private fun round2(value: Double) = (value * 100).roundToLong() / 100.0
    }
}
