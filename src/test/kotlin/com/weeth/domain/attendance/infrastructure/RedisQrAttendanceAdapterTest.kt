package com.weeth.domain.attendance.infrastructure

import com.weeth.config.TestContainersConfig
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfig::class)
class RedisQrAttendanceAdapterTest(
    private val redisQrAttendanceAdapter: RedisQrAttendanceAdapter,
    private val redisTemplate: RedisTemplate<String, String>,
) : DescribeSpec({
        beforeTest {
            redisTemplate.connectionFactory
                ?.connection
                ?.serverCommands()
                ?.flushDb()
        }

        describe("store") {
            it("QR 코드와 현재 활성 QR 세션을 함께 저장한다") {
                redisQrAttendanceAdapter.store(clubId = 7L, sessionId = 42L, code = 123456)

                redisQrAttendanceAdapter.getCode(42L) shouldBe 123456
                redisQrAttendanceAdapter.getActiveSessionId(7L) shouldBe 42L
                redisQrAttendanceAdapter.getExpiredAt(42L).shouldNotBeNull()
            }
        }

        describe("clearActiveSessionIfMatches") {
            it("현재 활성 QR 세션이 일치하면 active key를 삭제하고 true를 반환한다") {
                redisQrAttendanceAdapter.store(clubId = 7L, sessionId = 42L, code = 123456)

                val result = redisQrAttendanceAdapter.clearActiveSessionIfMatches(clubId = 7L, sessionId = 42L)

                result shouldBe true
                redisQrAttendanceAdapter.getActiveSessionId(7L) shouldBe null
            }

            it("현재 활성 QR 세션이 일치하지 않으면 active key를 삭제하지 않고 false를 반환한다") {
                redisQrAttendanceAdapter.store(clubId = 7L, sessionId = 99L, code = 123456)

                val result = redisQrAttendanceAdapter.clearActiveSessionIfMatches(clubId = 7L, sessionId = 42L)

                result shouldBe false
                redisQrAttendanceAdapter.getActiveSessionId(7L) shouldBe 99L
            }
        }
    })
