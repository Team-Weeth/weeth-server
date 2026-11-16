package com.weeth.domain.minigame.domain.vo

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonValue

enum class WaxPlayerStatus(
    @get:JsonValue val value: String,
) {
    WALKING("walking"),
    SEATED("seated"),

    /** 소음이 들켜 교수님이 다가오는 중. 누르기·이동이 막힌다. */
    SPOTTED("spotted"),
    WHACKED("whacked"),
}

enum class WaxProfessorMode(
    @get:JsonValue val value: String,
) {
    WRITING("writing"),
    TURNING("turning"),
    WATCHING("watching"),
    CHASING("chasing"),
    WHACKING("whacking"),
    RETURNING("returning"),
}

/** 플레이어끼리 주고받는 짧은 의사 표현. 채팅 대신 정해진 몇 가지만 보낸다. */
enum class WaxEmote(
    @get:JsonValue val value: String,
) {
    WAVE("wave"),
    LAUGH("laugh"),
    THUMBS("thumbs"),
    SHH("shh"),
    HELP("help"),
    ;

    companion object {
        fun from(value: String) = requireNotNull(entries.find { it.value == value }) { "없는 표현이에요." }
    }
}

data class WaxPublicPlayer(
    val id: String,
    val name: String,
    val character: String,
    val x: Double,
    val z: Double,
    val yaw: Double,
    val room: Int,
    // 아래 필드는 기본값(null·false)이면 생략한다. 매 스냅샷 × 인원수만큼 반복되는 값이라 전송량에 크게 영향을 준다.
    // 클라이언트는 받은 뒤 기본값을 채운다.
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val seat: Int?,
    val status: WaxPlayerStatus,
    @field:JsonInclude(JsonInclude.Include.NON_DEFAULT)
    val pressing: Boolean,
    /** 최근 EMOTE_VISIBLE_MS 안에 보낸 표현. */
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val emote: WaxEmote?,
)

data class WaxPrivatePlayer(
    val id: String,
    val room: Int,
    val seat: Int?,
    val x: Double,
    val z: Double,
    val status: WaxPlayerStatus,
    val pressing: Boolean,
    val noise: Double,
    val crack: Double,
    val layer: Int,
    val smallCracks: Int,
    val bigCracks: Int,
    val caughts: Int,
)

data class WaxProfessorState(
    val room: Int,
    val mode: WaxProfessorMode,
    val x: Double,
    val z: Double,
    val yaw: Double,
    val targetId: String?,
)

data class WaxEjection(
    val id: String,
    val name: String,
    val room: Int,
    val at: Long,
)

/** 같은 월드의 모든 연결이 함께 받는 부분. 브로드캐스트마다 월드당 한 번만 직렬화한다. */
data class WaxPublicState(
    val now: Long,
    val players: List<WaxPublicPlayer>,
    val professors: List<WaxProfessorState>,
    val ejections: List<WaxEjection>,
)

data class WaxSnapshot(
    val now: Long,
    val players: List<WaxPublicPlayer>,
    val professors: List<WaxProfessorState>,
    val you: WaxPrivatePlayer,
    val ejections: List<WaxEjection>,
    val type: String = "state",
)

data class WaxWelcome(
    val worldId: String,
    val you: String,
    val layout: WaxLayout,
    /** 입력이 없으면 내보내기까지의 시간. 클라이언트가 내보내기 전 경고를 띄우는 데 쓴다(전송 계층이 채운다). */
    val idleTimeoutMs: Long? = null,
    val type: String = "welcome",
)
