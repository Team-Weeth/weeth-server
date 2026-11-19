package com.weeth.domain.minigame.domain.vo

import java.security.MessageDigest
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** 한 판(입장~퇴장)이 끝났을 때의 기록. */
data class WaxPlayResult(
    val name: String,
    val brokenWaxes: Int,
)

/**
 * 로그인 없는 게임에서 같은 기기를 같은 사람으로 보기 위해 브라우저가 만들어 저장한 무작위 값.
 * 랭킹에는 원문 대신 [hash]만 저장해, 저장소 내용이 새어도 남의 기록을 이어 쓸 수 없게 한다.
 */
@JvmInline
value class WaxPlayerToken(
    val value: String,
) {
    init {
        require(PATTERN.matches(value)) { "기기 토큰 형식이 올바르지 않아요." }
    }

    fun hash(): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }

    companion object {
        private val PATTERN = Regex("^[A-Za-z0-9-]{16,64}$")

        fun parseOrNull(value: String?): WaxPlayerToken? = value?.let { runCatching { WaxPlayerToken(it) }.getOrNull() }
    }
}

/** 랭킹 집계 기간. 한국 시간 기준으로 일간은 자정, 주간은 월요일 자정에 새로 시작한다. */
enum class WaxRankingPeriod(
    private val length: Duration,
    /** 기간이 끝난 뒤에도 잠시 조회할 수 있게 기간보다 하루 길게 보관한다. */
    val retention: Duration,
) {
    DAILY(Duration.ofDays(1), Duration.ofDays(2)) {
        override fun startOf(date: LocalDate): LocalDate = date
    },
    WEEKLY(Duration.ofDays(7), Duration.ofDays(8)) {
        override fun startOf(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    },
    ;

    protected abstract fun startOf(date: LocalDate): LocalDate

    fun bucketOf(at: Instant): WaxRankingBucket {
        val start = startOf(at.atZone(ZONE).toLocalDate())
        val startAt = start.atStartOfDay(ZONE)
        return WaxRankingBucket(this, start, startAt.plus(length).toInstant())
    }

    companion object {
        val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    }
}

/** 특정 기간 한 칸(예: 2026-11-18 일간). */
data class WaxRankingBucket(
    val period: WaxRankingPeriod,
    val startDate: LocalDate,
    val endsAt: Instant,
) {
    val id: String get() = "${period.name.lowercase()}:$startDate"

    /**
     * 깬 왁스 수를 앞자리에, 기간 종료까지 남은 초를 뒷자리에 넣어 한 숫자로 정렬한다.
     * 같은 개수면 먼저 기록한 쪽(남은 시간이 더 긴 쪽)이 높은 점수가 되어 위에 온다.
     */
    fun scoreOf(
        brokenWaxes: Int,
        recordedAt: Instant,
    ): Double {
        val remaining = Duration.between(recordedAt, endsAt).seconds.coerceIn(0, TIE_BREAK_SCALE - 1)
        return brokenWaxes.toDouble() * TIE_BREAK_SCALE + remaining
    }

    companion object {
        /** 주간 기간의 초(604,800)보다 커야 깬 왁스 수 자리와 겹치지 않는다. */
        const val TIE_BREAK_SCALE = 1_000_000L

        fun brokenWaxesOf(score: Double): Int = (score / TIE_BREAK_SCALE).toInt()
    }
}

/** 랭킹에 반영할 한 판 점수. */
data class WaxScore(
    val tokenHash: String,
    val name: String,
    val brokenWaxes: Int,
    val recordedAt: Instant,
)

/** 랭킹 한 줄. [rank]는 1부터 시작한다. */
data class WaxRankEntry(
    val rank: Int,
    val name: String,
    val brokenWaxes: Int,
)
