package com.weeth.global.auth.jwt.infrastructure.store

import com.weeth.config.TestContainersConfig
import com.weeth.global.auth.jwt.application.exception.InvalidTokenException
import com.weeth.global.auth.jwt.application.exception.RedisTokenNotFoundException
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
            it("실제 Redis에 role/email/token을 저장하고 조회한다") {
                redisRefreshTokenStoreAdapter.save(1L, "rt", "ADMIN", "a@weeth.com")

                redisRefreshTokenStoreAdapter.getRole(1L) shouldBe "ADMIN"
                redisRefreshTokenStoreAdapter.getEmail(1L) shouldBe "a@weeth.com"
                redisTemplate.opsForHash<String, String>().get("refreshToken:1", "token") shouldBe "rt"
            }
        }

        describe("validateRefreshToken") {
            it("저장된 토큰과 일치하면 예외가 발생하지 않는다") {
                redisRefreshTokenStoreAdapter.save(2L, "stored", "USER", "u@weeth.com")

                redisRefreshTokenStoreAdapter.validateRefreshToken(2L, "stored")
            }

            it("요청 토큰이 다르면 InvalidTokenException이 발생한다") {
                redisRefreshTokenStoreAdapter.save(3L, "stored", "USER", "u@weeth.com")

                shouldThrow<InvalidTokenException> {
                    redisRefreshTokenStoreAdapter.validateRefreshToken(3L, "different")
                }
            }
        }

        describe("getRole/getEmail") {
            it("값이 없으면 RedisTokenNotFoundException이 발생한다") {
                shouldThrow<RedisTokenNotFoundException> {
                    redisRefreshTokenStoreAdapter.getRole(999L)
                }
                shouldThrow<RedisTokenNotFoundException> {
                    redisRefreshTokenStoreAdapter.getEmail(999L)
                }
            }
        }

        describe("delete/updateRole") {
            it("delete 후 조회 시 예외가 발생한다") {
                redisRefreshTokenStoreAdapter.save(4L, "rt", "USER", "x@weeth.com")
                redisRefreshTokenStoreAdapter.delete(4L)

                shouldThrow<RedisTokenNotFoundException> {
                    redisRefreshTokenStoreAdapter.getRole(4L)
                }
            }

            it("updateRole은 기존 저장 값의 role만 변경한다") {
                redisRefreshTokenStoreAdapter.save(5L, "rt", "USER", "x@weeth.com")

                redisRefreshTokenStoreAdapter.updateRole(5L, "ADMIN")

                redisRefreshTokenStoreAdapter.getRole(5L) shouldBe "ADMIN"
                redisRefreshTokenStoreAdapter.getEmail(5L) shouldBe "x@weeth.com"
            }
        }
    }) {
    companion object {
        private const val PREFIX = "refreshToken:"
    }
}
