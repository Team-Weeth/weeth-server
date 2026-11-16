package com.weeth.domain.minigame.domain.entity

import com.weeth.domain.minigame.domain.vo.WaxEmote
import com.weeth.domain.minigame.domain.vo.WaxLayout
import com.weeth.domain.minigame.domain.vo.WaxPlayerStatus
import com.weeth.domain.minigame.domain.vo.WaxProfessorMode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.math.hypot
import kotlin.random.Random

class WaxWorldTest :
    StringSpec({
        val layout = WaxLayout.DEFAULT
        val room = layout.room(1)

        /** 직선으로 0.5m씩 걷는다. 막히면 실패한다. */
        fun WaxWorld.stepTo(
            id: String,
            x: Double,
            z: Double,
        ) {
            var me = snapshot(id, 0).you
            while (hypot(x - me.x, z - me.z) > 0.01) {
                val length = hypot(x - me.x, z - me.z)
                val step = minOf(0.5, length)
                move(id, me.x + (x - me.x) / length * step, me.z + (z - me.z) / length * step, 0.0)
                val next = snapshot(id, 0).you
                check(next.x != me.x || next.z != me.z) { "이동이 막혔어요: (${me.x}, ${me.z}) → ($x, $z)" }
                me = next
            }
        }

        /** 복도에서 문을 지나 강의실 안 목표 지점까지 걷는다. */
        fun WaxWorld.walkTo(
            id: String,
            x: Double,
            z: Double,
        ) {
            stepTo(id, room.doorX, 1.0)
            stepTo(id, room.doorX, -1.0)
            stepTo(id, x, -1.0)
            stepTo(id, x, z)
        }

        fun seatedWorld(vararg seats: Pair<String, Int>): WaxWorld {
            val world = WaxWorld("test", Random(7))
            seats.forEach { (id, seat) ->
                world.join(id)
                world.walkTo(id, room.seats[seat].x + 1.5, room.seats[seat].z)
                world.sit(id, room.id, seat)
            }
            return world
        }

        "입장하면 학교 테마 후보 중 하나를 닉네임으로 받고, 같은 교실에서는 겹치지 않는다" {
            val world = WaxWorld("names", Random(3), capacity = WaxWorld.NICKNAMES.size)
            WaxWorld.NICKNAMES.indices.forEach { world.join("p$it") }

            val names = world.publicState(0).players.map { it.name }
            names.toSet() shouldBe WaxWorld.NICKNAMES.toSet()
        }

        "후보를 모두 쓴 교실에서도 입장할 수 있다" {
            val world = WaxWorld("names", Random(3), capacity = WaxWorld.NICKNAMES.size + 1)
            (0..WaxWorld.NICKNAMES.size).forEach { world.join("p$it") }

            world
                .publicState(0)
                .players
                .last()
                .name shouldBeIn WaxWorld.NICKNAMES
        }

        "복도에서 문을 지나 걸어 들어가면 좌표로 강의실을 판정한다" {
            val world = WaxWorld("test", Random(1))
            world.join("a")
            world.snapshot("a", 0).you.room shouldBe 0
            world.walkTo("a", room.seats[0].x + 1.5, room.seats[0].z)
            world.snapshot("a", 0).you.room shouldBe room.id
        }

        "벽을 통과하거나 한 번에 멀리 이동하는 좌표는 반영하지 않는다" {
            val world = WaxWorld("test", Random(1))
            world.join("a")
            // 문이 아닌 벽을 넘어 강의실로 들어가는 이동
            world.stepTo("a", room.doorX - 3.0, 0.5)
            world.move("a", room.doorX - 3.0, -0.5, 0.0)
            world.snapshot("a", 0).you.room shouldBe 0
            world.snapshot("a", 0).you.z shouldBe 0.5
            // 순간이동
            world.move("a", room.doorX + 2.0, 0.5, 0.0)
            world.snapshot("a", 0).you.x shouldBe room.doorX - 3.0
            // 복도 바깥
            world.stepTo("a", room.doorX - 3.0, 8.5)
            world.move("a", room.doorX - 3.0, 9.3, 0.0)
            world.snapshot("a", 0).you.z shouldBe 8.5
            shouldThrow<IllegalArgumentException> { world.move("a", Double.NaN, 0.0, 0.0) }
        }

        "의자 근처의 빈 좌석에만 앉을 수 있고 일어나면 다시 비어 있다" {
            val world = WaxWorld("test", Random(1))
            world.join("a")
            world.join("b")
            shouldThrow<IllegalArgumentException> { world.sit("a", room.id, 0) }
            world.walkTo("a", room.seats[0].x + 1.5, room.seats[0].z)
            world.walkTo("b", room.seats[0].x + 1.5, room.seats[0].z)
            shouldThrow<IllegalArgumentException> { world.sit("a", room.id, 8) }
            world.sit("a", room.id, 0)
            shouldThrow<IllegalArgumentException> { world.sit("b", room.id, 0) }
            world.stand("a")
            world.snapshot("a", 0).you.seat shouldBe null
            world.sit("b", room.id, 0)
            world.snapshot("b", 0).you.status shouldBe WaxPlayerStatus.SEATED
        }

        "누르면 작은 균열 뒤 큰 깨짐이 나고 왁뿌볼 종류마다 속도가 다르다" {
            val soft = room.seats.first { layout.variants[it.variant].hardness >= 46.0 }
            val hard = room.seats.first { layout.variants[it.variant].hardness <= 30.0 }
            val world = seatedWorld("soft" to soft.index, "hard" to hard.index)
            for (now in 50L..1500L step 50) {
                world.press("soft", true, now)
                world.press("hard", true, now)
                world.tick(now, 0.05)
            }
            val softState = world.snapshot("soft", 1500).you
            val hardState = world.snapshot("hard", 1500).you
            softState.smallCracks shouldBe 2
            (softState.smallCracks > hardState.smallCracks) shouldBe true
            for (now in 1550L..2600L step 50) {
                world.press("soft", true, now)
                world.tick(now, 0.05)
            }
            world.snapshot("soft", 2600).you.bigCracks shouldBeGreaterThan 0
            world.snapshot("soft", 2600).you.layer shouldBeGreaterThan 0
        }

        "손을 떼면 소음이 내려가고 입력이 끊기면 자동으로 손을 뗀다" {
            val world = seatedWorld("a" to 0)
            for (now in 50L..600L step 50) {
                world.press("a", true, now)
                world.tick(now, 0.05)
            }
            val loud = world.snapshot("a", 600).you.noise
            loud shouldBeGreaterThan 0.0
            world.press("a", false, 600)
            for (now in 650L..2000L step 50) world.tick(now, 0.05)
            world.snapshot("a", 2000).you.noise shouldBeLessThan loud
            world.press("a", true, 2000)
            for (now in 2050L..3500L step 50) world.tick(now, 0.05)
            world.snapshot("a", 3500).you.pressing shouldBe false
        }

        "소음이 넘치면 교수님이 다가와 때리고 본인만 문 앞 복도로 쫓겨난다" {
            val world = seatedWorld("a" to 4, "b" to 5)
            var now = 0L
            var chased = false
            var whacked = false
            while (now < 20000 && world.snapshot("a", now).you.caughts == 0) {
                now += 50
                if (world.snapshot("a", now).you.status == WaxPlayerStatus.SEATED) world.press("a", true, now)
                world.tick(now, 0.05)
                val snapshot = world.snapshot("b", now)
                val professor = snapshot.professors.first { it.room == room.id }
                if (professor.mode == WaxProfessorMode.CHASING && professor.targetId == "a") chased = true
                if (snapshot.players.first { it.id == "a" }.status == WaxPlayerStatus.WHACKED) whacked = true
            }
            chased shouldBe true
            whacked shouldBe true
            val me = world.snapshot("a", now).you
            me.caughts shouldBe 1
            me.room shouldBe 0
            me.seat shouldBe null
            me.noise shouldBe 0.0
            me.x shouldBe room.ejectPoint.x
            me.z shouldBe room.ejectPoint.z
            val other = world.snapshot("b", now)
            other.you.seat shouldBe 5
            other.ejections.map { it.id } shouldBe listOf("a")
            (other.players.first { it.id == "b" }.status) shouldBe WaxPlayerStatus.SEATED
        }

        "교수님이 돌아보는 중이면 같은 입력에도 소음이 더 빨리 오른다" {
            fun noiseAfterPress(mode: WaxProfessorMode): Double {
                val world = seatedWorld("a" to 0)
                var now = 0L
                while (world
                        .snapshot("a", now)
                        .professors
                        .first { it.room == room.id }
                        .mode != mode
                ) {
                    now += 50
                    world.tick(now, 0.05)
                }
                repeat(4) {
                    now += 50
                    world.press("a", true, now)
                    world.tick(now, 0.05)
                }
                return world.snapshot("a", now).you.noise
            }
            noiseAfterPress(WaxProfessorMode.WATCHING) shouldBeGreaterThan noiseAfterPress(WaxProfessorMode.WRITING)
        }

        "개인 소음과 정원을 지킨다" {
            val world = WaxWorld("test", Random(1))
            (1..WaxWorld.MAX_PLAYERS).forEach { world.join("guest-$it") }
            shouldThrow<IllegalArgumentException> { world.join("overflow") }
            shouldThrow<IllegalArgumentException> { world.press("guest-1", true, 0) }
        }

        "학생은 5종 캐릭터 중 하나로 입장하고 강의실마다 교수님 캐릭터가 다르다" {
            val world = WaxWorld("test", Random(3))
            (1..WaxWorld.MAX_PLAYERS).forEach { world.join("guest-$it") }
            val characters =
                world
                    .snapshot("guest-1", 0)
                    .players
                    .map { it.character }
                    .toSet()
            WaxLayout.STUDENT_CHARACTERS.size shouldBe 5
            WaxLayout.STUDENT_CHARACTERS.containsAll(characters) shouldBe true
            (characters.size > 1) shouldBe true
            val professors = layout.rooms.map { it.professorCharacter }
            professors.toSet().size shouldBe layout.rooms.size
            professors.none { it in WaxLayout.STUDENT_CHARACTERS } shouldBe true
            layout.rooms
                .map { it.subject }
                .toSet()
                .size shouldBe layout.rooms.size
        }

        "표현은 다른 플레이어에게 잠깐 보였다가 사라지고 연타는 쿨다운 동안 무시한다" {
            val world = WaxWorld("test", Random(1))
            world.join("a")
            world.join("b")

            fun seenByB(now: Long) =
                world
                    .snapshot("b", now)
                    .players
                    .first { it.id == "a" }
                    .emote

            world.emote("a", WaxEmote.WAVE, 1_000)
            seenByB(1_000) shouldBe WaxEmote.WAVE
            world.emote("a", WaxEmote.LAUGH, 1_000 + WaxWorld.EMOTE_COOLDOWN_MS - 1)
            seenByB(1_500) shouldBe WaxEmote.WAVE
            world.emote("a", WaxEmote.LAUGH, 1_000 + WaxWorld.EMOTE_COOLDOWN_MS)
            seenByB(2_000) shouldBe WaxEmote.LAUGH
            seenByB(1_000 + WaxWorld.EMOTE_COOLDOWN_MS + WaxWorld.EMOTE_VISIBLE_MS) shouldBe null
        }

        "앉아 있거나 들킨 중에도 표현을 보낼 수 있고 없는 표현은 거부한다" {
            val world = seatedWorld("a" to 0)
            world.emote("a", WaxEmote.HELP, 10)
            world
                .snapshot("a", 10)
                .players
                .first()
                .emote shouldBe WaxEmote.HELP
            shouldThrow<IllegalArgumentException> { WaxEmote.from("dance") }
        }

        "왁뿌볼 종류는 과일·음식 이름 없이 9종이다" {
            val food = listOf("복숭아", "말차", "레몬", "블루베리", "초코", "수박", "바닐라", "딸기", "포도", "우유", "라떼", "쿠키", "젤리")
            layout.variants.size shouldBe 9
            layout.variants.none { variant -> food.any { it in variant.name || it in variant.note } } shouldBe true
        }
    })
