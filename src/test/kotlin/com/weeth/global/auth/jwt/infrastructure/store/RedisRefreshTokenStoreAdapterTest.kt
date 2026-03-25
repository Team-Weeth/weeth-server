package com.weeth.global.auth.jwt.infrastructure.store

import com.weeth.config.TestContainersConfig
import com.weeth.global.auth.jwt.application.exception.InvalidTokenException
import com.weeth.global.auth.jwt.application.exception.RedisTokenNotFoundException
import com.weeth.global.auth.jwt.domain.enums.TokenType
import com.weeth.global.auth.jwt.infrastructure.RedisRefreshTokenStoreAdapter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfig::class)
class RedisRefreshTokenStoreAdapterTest(
    private val redisRefreshTokenStoreAdapter: RedisRefreshTokenStoreAdapter,
    private val redisTemplate: RedisTemplate<String, String>,
) : DescribeSpec({
        beforeTest {
            val keys = redisTemplate.keys("$PREFIX*")
            if (!keys.isNullOrEmpty()) {
                redisTemplate.delete(keys)
            }
        }

        describe("save/get") {
            it("실제 Redis에 email/token/tokenType을 저장하고 조회한다") {
                redisRefreshTokenStoreAdapter.save(1L, "rt", "a@weeth.com", TokenType.ACCESS)

                redisRefreshTokenStoreAdapter.getEmail(1L) shouldBe "a@weeth.com"
                redisRefreshTokenStoreAdapter.getTokenType(1L) shouldBe TokenType.ACCESS
                redisTemplate.opsForHash<String, String>().get("refreshToken:1", "token") shouldBe "rt"
                redisTemplate.opsForHash<String, String>().get("refreshToken:1", "tokenType") shouldBe "ACCESS"
            }

            it("TEMPORARY tokenType을 저장하고 조회한다") {
                redisRefreshTokenStoreAdapter.save(5L, "rt", "new@weeth.com", TokenType.TEMPORARY)

                redisRefreshTokenStoreAdapter.getTokenType(5L) shouldBe TokenType.TEMPORARY
            }
        }

        describe("validateRefreshToken") {
            it("저장된 토큰과 일치하면 예외가 발생하지 않는다") {
                redisRefreshTokenStoreAdapter.save(2L, "stored", "u@weeth.com", TokenType.ACCESS)

                redisRefreshTokenStoreAdapter.validateRefreshToken(2L, "stored")
            }

            it("요청 토큰이 다르면 InvalidTokenException이 발생한다") {
                redisRefreshTokenStoreAdapter.save(3L, "stored", "u@weeth.com", TokenType.ACCESS)

                shouldThrow<InvalidTokenException> {
                    redisRefreshTokenStoreAdapter.validateRefreshToken(3L, "different")
                }
            }
        }

        describe("getEmail") {
            it("값이 없으면 RedisTokenNotFoundException이 발생한다") {
                shouldThrow<RedisTokenNotFoundException> {
                    redisRefreshTokenStoreAdapter.getEmail(999L)
                }
            }
        }

        describe("getTokenType") {
            it("값이 없으면 기본값 ACCESS를 반환한다") {
                // tokenType 필드가 없는 기존 데이터 시뮬레이션
                val key = "refreshToken:998"
                redisTemplate.opsForHash<String, String>().putAll(
                    key,
                    mapOf("token" to "rt", "email" to "old@weeth.com"),
                )

                redisRefreshTokenStoreAdapter.getTokenType(998L) shouldBe TokenType.ACCESS
            }
        }

        describe("delete") {
            it("delete 후 조회 시 예외가 발생한다") {
                redisRefreshTokenStoreAdapter.save(4L, "rt", "x@weeth.com", TokenType.ACCESS)
                redisRefreshTokenStoreAdapter.delete(4L)

                shouldThrow<RedisTokenNotFoundException> {
                    redisRefreshTokenStoreAdapter.getEmail(4L)
                }
            }
        }
    }) {
    companion object {
        private const val PREFIX = "refreshToken:"
    }
}
