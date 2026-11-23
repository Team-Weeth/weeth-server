package com.weeth.domain.minigame.infrastructure

import com.weeth.config.TestContainersConfig
import com.weeth.domain.minigame.domain.vo.WaxRankEntry
import com.weeth.domain.minigame.domain.vo.WaxRankingPeriod
import com.weeth.domain.minigame.domain.vo.WaxScore
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.longs.shouldBeInRange
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.time.ZoneId

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfig::class)
class RedisWaxRankingAdapterTest(
    private val adapter: RedisWaxRankingAdapter,
    private val redisTemplate: RedisTemplate<String, String>,
) : DescribeSpec({
        val wednesday = LocalDateTime.parse("2026-11-18T12:00:00").atZone(ZoneId.of("Asia/Seoul")).toInstant()
        val daily = WaxRankingPeriod.DAILY.bucketOf(wednesday)
        val weekly = WaxRankingPeriod.WEEKLY.bucketOf(wednesday)

        fun score(
            token: String,
            name: String,
            brokenWaxes: Int,
            secondsLater: Long = 0,
        ) = WaxScore(token, name, brokenWaxes, wednesday.plusSeconds(secondsLater))

        beforeTest {
            redisTemplate.execute<Unit> { connection -> connection.serverCommands().flushDb() }
        }

        describe("record") {
            it("일간·주간 랭킹에 함께 반영하고 상위 순으로 돌려준다") {
                adapter.record(score("a", "새내기", 3))
                adapter.record(score("b", "복학생", 7))

                val expected = listOf(WaxRankEntry(1, "복학생", 7), WaxRankEntry(2, "새내기", 3))
                adapter.top(daily, 10) shouldBe expected
                adapter.top(weekly, 10) shouldBe expected
            }

            it("기존 기록보다 낮으면 점수는 그대로 두고 닉네임만 최근 값으로 바꾼다") {
                adapter.record(score("a", "새내기", 5))
                adapter.record(score("a", "개명한새내기", 2, secondsLater = 60))

                adapter.find(daily, "a") shouldBe WaxRankEntry(1, "개명한새내기", 5)
            }

            it("기존 기록보다 높으면 점수를 갱신한다") {
                adapter.record(score("a", "새내기", 5))
                adapter.record(score("a", "새내기", 9, secondsLater = 60))

                adapter.find(weekly, "a") shouldBe WaxRankEntry(1, "새내기", 9)
            }

            it("같은 개수면 먼저 기록한 사람이 위에 온다") {
                adapter.record(score("late", "늦은사람", 4, secondsLater = 600))
                adapter.record(score("early", "빠른사람", 4))

                adapter.top(daily, 10).map { it.name } shouldBe listOf("빠른사람", "늦은사람")
            }

            it("기간 키에 보관 기간만큼 만료를 건다") {
                adapter.record(score("a", "새내기", 1))

                redisTemplate.getExpire("wax:rank:${daily.id}") shouldBeInRange
                    (1L..WaxRankingPeriod.DAILY.retention.seconds)
                redisTemplate.getExpire("wax:rank:${weekly.id}:names") shouldBeInRange
                    (1L..WaxRankingPeriod.WEEKLY.retention.seconds)
            }
        }

        describe("top / find") {
            it("limit만큼만 돌려주고, 기록이 없으면 빈 목록과 null이다") {
                adapter.top(daily, 10) shouldBe emptyList()
                adapter.find(daily, "none") shouldBe null
                (1..5).forEach { adapter.record(score("p$it", "플레이어$it", it)) }

                adapter.top(daily, 3).map { it.brokenWaxes } shouldBe listOf(5, 4, 3)
                adapter.find(daily, "p1") shouldBe WaxRankEntry(5, "플레이어1", 1)
            }
        }
    })
