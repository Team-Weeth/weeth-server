package com.weeth.global.config.properties

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class UniversityManualMajorConfigTest :
    DescribeSpec({
        val objectMapper = jacksonObjectMapper()

        describe("manualMajors") {
            it("JSON 배열 환경변수 값을 학과 목록으로 변환한다") {
                val properties =
                    UniversityProperties(
                        manualMajorsJson =
                            """[{"name":"인공지능학과","category":"공학계열"},{"name":"AI융합학과","category":"공학계열"}]""",
                    )

                val config = UniversityManualMajorConfig(properties, objectMapper)

                config.manualMajors shouldBe
                    listOf(
                        UniversityProperties.ManualMajor("인공지능학과", "공학계열"),
                        UniversityProperties.ManualMajor("AI융합학과", "공학계열"),
                    )
            }

            it("JSON 형식이 올바르지 않으면 설정 생성을 실패한다") {
                val properties = UniversityProperties(manualMajorsJson = "not-json")

                shouldThrow<IllegalArgumentException> {
                    UniversityManualMajorConfig(properties, objectMapper)
                }
            }
        }
    })
