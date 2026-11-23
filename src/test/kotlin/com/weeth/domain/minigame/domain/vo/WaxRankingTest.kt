package com.weeth.domain.minigame.domain.vo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class WaxRankingTest :
    StringSpec({
        val kst = ZoneId.of("Asia/Seoul")

        fun at(text: String) = LocalDateTime.parse(text).atZone(kst).toInstant()

        "기기 토큰은 형식을 검사하고, 저장용 해시는 원문과 다르며 항상 같다" {
            val token = WaxPlayerToken("3f2b8c1e-6a4d-4e2b-9c1f-7d8e9a0b1c2d")
            token.hash() shouldBe token.hash()
            token.hash() shouldNotBe token.value
            token.hash().length shouldBe 64
            listOf("short", "has space in token value", "x".repeat(65), "토큰토큰토큰토큰토큰토큰토큰토큰").forEach {
                shouldThrow<IllegalArgumentException> { WaxPlayerToken(it) }
            }
            WaxPlayerToken.parseOrNull("short") shouldBe null
            WaxPlayerToken.parseOrNull(null) shouldBe null
        }

        "일간 기간은 한국 시간 자정에 바뀐다" {
            val late = WaxRankingPeriod.DAILY.bucketOf(at("2026-11-18T23:59:59"))
            late.startDate shouldBe LocalDate.parse("2026-11-18")
            late.endsAt shouldBe at("2026-11-19T00:00:00")
            WaxRankingPeriod.DAILY.bucketOf(at("2026-11-19T00:00:00")).startDate shouldBe LocalDate.parse("2026-11-19")
            // UTC로는 아직 18일 15시지만 한국 시간으로는 19일 0시다.
            WaxRankingPeriod.DAILY.bucketOf(at("2026-11-19T00:00:00")).id shouldBe "daily:2026-11-19"
        }

        "주간 기간은 월요일 0시에 시작한다" {
            val wednesday = WaxRankingPeriod.WEEKLY.bucketOf(at("2026-11-18T12:00:00"))
            wednesday.startDate shouldBe LocalDate.parse("2026-11-16")
            wednesday.endsAt shouldBe at("2026-11-23T00:00:00")
            WaxRankingPeriod.WEEKLY.bucketOf(at("2026-11-22T23:59:59")).startDate shouldBe LocalDate.parse("2026-11-16")
            WaxRankingPeriod.WEEKLY.bucketOf(at("2026-11-23T00:00:00")).startDate shouldBe LocalDate.parse("2026-11-23")
        }

        "깬 왁스가 많을수록, 같으면 먼저 기록할수록 점수가 높고, 점수에서 깬 왁스 수를 되돌릴 수 있다" {
            val bucket = WaxRankingPeriod.WEEKLY.bucketOf(at("2026-11-18T12:00:00"))
            val early = bucket.scoreOf(5, at("2026-11-16T00:00:01"))
            val late = bucket.scoreOf(5, at("2026-11-22T23:59:59"))
            val more = bucket.scoreOf(6, at("2026-11-22T23:59:59"))
            early shouldBeGreaterThan late
            more shouldBeGreaterThan early
            WaxRankingBucket.brokenWaxesOf(early) shouldBe 5
            WaxRankingBucket.brokenWaxesOf(late) shouldBe 5
            WaxRankingBucket.brokenWaxesOf(more) shouldBe 6
        }

        "같은 초 안에서도 먼저 기록한 쪽이 높은 점수가 된다" {
            val bucket = WaxRankingPeriod.DAILY.bucketOf(at("2026-11-18T12:00:00"))
            val first = bucket.scoreOf(5, at("2026-11-18T12:00:00.100"))
            val second = bucket.scoreOf(5, at("2026-11-18T12:00:00.200"))
            first shouldBeGreaterThan second
            WaxRankingBucket.brokenWaxesOf(first) shouldBe 5
        }
    })
