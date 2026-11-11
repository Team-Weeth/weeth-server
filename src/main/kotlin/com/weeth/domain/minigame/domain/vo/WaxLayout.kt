package com.weeth.domain.minigame.domain.vo

import kotlin.math.abs
import kotlin.math.hypot

data class WaxRect(
    val minX: Double,
    val maxX: Double,
    val minZ: Double,
    val maxZ: Double,
) {
    fun contains(
        x: Double,
        z: Double,
        margin: Double = 0.0,
    ) = x in (minX + margin)..(maxX - margin) && z in (minZ + margin)..(maxZ - margin)
}

/** 책상마다 다른 왁뿌볼(색 조합·층·마감). hardness는 초당 균열 진행량, loudness는 깨질 때 소음 배율이다. */
data class WaxVariant(
    val id: Int,
    val name: String,
    val note: String,
    val hardness: Double,
    val loudness: Double,
)

data class WaxSeat(
    val index: Int,
    val x: Double,
    val z: Double,
    val deskX: Double,
    val deskZ: Double,
    val variant: Int,
)

data class WaxPoint(
    val x: Double,
    val z: Double,
)

/** 강의실마다 다른 과목·담당 교수님. professorCharacter는 Kenney Mini Characters 모델 이름이다. */
data class WaxRoom(
    val id: Int,
    val name: String,
    val subject: String,
    val professorName: String,
    val professorCharacter: String,
    val bounds: WaxRect,
    val doorX: Double,
    val professorHome: WaxPoint,
    val seats: List<WaxSeat>,
) {
    /** 쫓겨난 플레이어가 서는 복도 쪽 문 앞 위치. */
    val ejectPoint get() = WaxPoint(doorX, WaxLayout.HALL.minZ + 1.4)
}

/** 복도(z 0~9) 북쪽으로 강의실 3개가 붙은 한 층. -z가 칠판 방향이다. */
data class WaxLayout(
    val hall: WaxRect,
    val rooms: List<WaxRoom>,
    val variants: List<WaxVariant>,
    val doorWidth: Double,
    val wallZ: Double,
) {
    fun roomAt(
        x: Double,
        z: Double,
    ): Int = rooms.firstOrNull { it.bounds.contains(x, z) }?.id ?: 0

    fun room(id: Int) = requireNotNull(rooms.firstOrNull { it.id == id }) { "없는 강의실이에요." }

    fun walkable(
        x: Double,
        z: Double,
    ) = hall.contains(x, z, BODY) || rooms.any { it.bounds.contains(x, z, BODY) } || inDoorway(x, z)

    /** 문 밖 벽을 통과하지 않는 한 걸음인지 판정한다. 한 걸음은 MAX_STEP 이하라 중간 지점만 확인한다. */
    fun canStep(
        fromX: Double,
        fromZ: Double,
        toX: Double,
        toZ: Double,
    ): Boolean {
        if (!walkable(toX, toZ) || hypot(toX - fromX, toZ - fromZ) > MAX_STEP) return false
        val from = roomAt(fromX, fromZ)
        val to = roomAt(toX, toZ)
        if (from == to) return true
        val crossingX = fromX + (toX - fromX) * crossingRatio(fromZ, toZ)
        val door = if (from != 0) room(from).doorX else room(to).doorX
        return abs(crossingX - door) <= doorWidth / 2 - BODY / 2
    }

    private fun crossingRatio(
        fromZ: Double,
        toZ: Double,
    ) = if (fromZ == toZ) 0.0 else ((wallZ - fromZ) / (toZ - fromZ)).coerceIn(0.0, 1.0)

    private fun inDoorway(
        x: Double,
        z: Double,
    ) = rooms.any { abs(x - it.doorX) <= doorWidth / 2 - BODY / 2 && abs(z - wallZ) <= 0.6 }

    companion object {
        const val MAX_STEP = 1.5
        const val SIT_DISTANCE = 2.5
        private const val BODY = 0.35

        val HALL = WaxRect(-17.5, 17.5, 0.0, 9.0)

        val VARIANTS =
            listOf(
                WaxVariant(0, "파스텔 핑크 3겹", "얇은 유광 왁스, 가볍게 파사삭", 46.0, 0.8),
                WaxVariant(1, "딥 그린 두꺼운 왁스", "무광 두꺼운 껍질, 묵직하게 빠자작", 30.0, 1.25),
                WaxVariant(2, "옐로 유광", "단단한 유광, 톡톡 터지는 바삭함", 52.0, 0.9),
                WaxVariant(3, "라벤더 무광", "부드러운 무광, 촘촘한 잔금", 40.0, 0.7),
                WaxVariant(4, "브라운 5겹", "다섯 겹 두꺼운 왁스, 우지끈", 26.0, 1.4),
                WaxVariant(5, "레드 마블", "마블 무늬 왁스, 시원하게 쩍", 38.0, 1.1),
                WaxVariant(6, "코튼 화이트", "하얀 무광, 부드럽게 바삭", 44.0, 0.75),
                WaxVariant(7, "핑크 펄", "펄 왁스 얇은 층이 겹겹이", 48.0, 0.95),
                WaxVariant(8, "퍼플 글리터", "반짝이 왁스, 버티다가 빠직", 34.0, 1.0),
            )

        private data class Subject(
            val name: String,
            val professorName: String,
            val professorCharacter: String,
        )

        private val SUBJECTS =
            listOf(
                Subject("일반화학", "화학과 김교수님", "male-e"),
                Subject("기초 드로잉", "디자인과 박교수님", "male-b"),
                Subject("음악의 이해", "음악과 최교수님", "male-d"),
            )

        /** 학생 캐릭터 5종(Kenney Mini Characters 모델 이름). 안경 쓴 캐릭터는 빼고, 교수님 캐릭터와 겹치지 않는다. */
        val STUDENT_CHARACTERS = listOf("female-b", "female-c", "female-e", "female-f", "male-f")

        val DEFAULT: WaxLayout = build()

        private fun build(): WaxLayout {
            val rooms =
                SUBJECTS.mapIndexed { index, subject ->
                    val id = index + 1
                    val centerX = (index - 1) * 11.5
                    val seats =
                        // 뒷줄 의자와 뒤쪽 사물함 사이에 지나다닐 통로를 남긴다.
                        listOf(-6.2, -4.3, -2.4).flatMapIndexed { row, deskZ ->
                            listOf(-3.0, 0.0, 3.0).mapIndexed { column, offset ->
                                val seat = row * 3 + column
                                WaxSeat(
                                    index = seat,
                                    x = centerX + offset,
                                    z = deskZ + 0.75,
                                    deskX = centerX + offset,
                                    deskZ = deskZ,
                                    variant = (seat + index * 4) % VARIANTS.size,
                                )
                            }
                        }
                    WaxRoom(
                        id = id,
                        name = "${101 + index}호",
                        subject = subject.name,
                        professorName = subject.professorName,
                        professorCharacter = subject.professorCharacter,
                        bounds = WaxRect(centerX - 5.25, centerX + 5.25, -10.0, 0.0),
                        // 뒷줄 책상과 겹치지 않도록 교실 뒤쪽 오른편 끝에 문을 둔다.
                        doorX = centerX + 4.25,
                        professorHome = WaxPoint(centerX, -8.6),
                        seats = seats,
                    )
                }
            return WaxLayout(HALL, rooms, VARIANTS, doorWidth = 1.8, wallZ = 0.0)
        }
    }
}
