package com.weeth.global.auth.jwt.infrastructure.store

import com.weeth.config.TestContainersConfig
import com.weeth.global.auth.jwt.infrastructure.RedisAccessTokenBlacklistStoreAdapter
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfig::class)
class RedisAccessTokenBlacklistStoreAdapterTest(
    private val redisAccessTokenBlacklistStoreAdapter: RedisAccessTokenBlacklistStoreAdapter,
    private val redisTemplate: RedisTemplate<String, String>,
) : DescribeSpec({
        beforeTest {
            val keys = redisTemplate.keys("$PREFIX*")
            if (!keys.isNullOrEmpty()) {
                redisTemplate.delete(keys)
            }
        }

        describe("blacklist") {
            it("사용자를 blacklist에 등록하고 TTL을 설정한다") {
                redisAccessTokenBlacklistStoreAdapter.blacklist(1L)

                redisAccessTokenBlacklistStoreAdapter.isBlacklisted(1L) shouldBe true
                redisTemplate.getExpire("${PREFIX}1") shouldBeGreaterThan 0L
            }
        }
    }) {
    companion object {
        private const val PREFIX = "accessTokenBlacklist:"
    }
}
