package com.weeth.global.config.properties

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.env.SystemEnvironmentPropertySource
import java.time.LocalDateTime

/**
 * application.yml 이 두 값을 `${ENV:}` 로 받기 때문에, 환경 변수를 주지 않은 환경에서는
 * 빈 문자열이 바인딩된다. 이때 바인딩이 실패하면 애플리케이션이 기동하지 못하므로
 * "값을 안 주면 그냥 꺼진다"가 실제로 성립하는지 확인한다.
 */
class LegacyContentPropertiesTest :
    StringSpec({
        fun bind(properties: Map<String, Any>): LegacyContentProperties {
            val environment = StandardEnvironment()
            environment.propertySources.addFirst(SystemEnvironmentPropertySource("test-env", properties))
            return Binder.get(environment).bindOrCreate("legacy-content", LegacyContentProperties::class.java)
        }

        "환경 변수를 주지 않으면 빈 값이 들어와도 기동에 실패하지 않고 변환이 꺼진다" {
            val properties =
                bind(
                    mapOf(
                        "LEGACY_CONTENT_EDITOR_MIGRATED_AT" to "",
                        "LEGACY_CONTENT_CLUB_ID" to "",
                    ),
                )

            properties.editorMigratedAt shouldBe null
            properties.clubId shouldBe null
            properties.isEnabled() shouldBe false
        }

        "환경 변수 이름으로 값을 주면 설정에 바인딩된다" {
            val properties =
                bind(
                    mapOf(
                        "LEGACY_CONTENT_EDITOR_MIGRATED_AT" to "2026-09-20T00:00:00",
                        "LEGACY_CONTENT_CLUB_ID" to "883010121028214452",
                    ),
                )

            properties.editorMigratedAt shouldBe LocalDateTime.of(2026, 9, 20, 0, 0)
            properties.clubId shouldBe 883010121028214452L
            properties.isEnabled() shouldBe true
        }

        "전환 시각만 주면 동아리를 가리지 않는 상태로 켜진다" {
            val properties = bind(mapOf("LEGACY_CONTENT_EDITOR_MIGRATED_AT" to "2026-09-20T00:00:00"))

            properties.editorMigratedAt shouldBe LocalDateTime.of(2026, 9, 20, 0, 0)
            properties.clubId shouldBe null
            properties.isEnabled() shouldBe true
        }
    })
