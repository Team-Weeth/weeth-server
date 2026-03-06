package com.weeth.global.common.id

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class TsidBase62EncoderTest :
    StringSpec({
        "Tsid Long이 Base62 String으로 정상 인코딩 된다." {
            val cases =
                mapOf(
                    0L to "0",
                    61L to "Z",
                    62L to "10",
                    375_109L to "1zA9",
                )

            cases.forEach { (tsid, expected) ->
                TsidBase62Encoder.encode(tsid) shouldBe expected
            }
        }

        "Base62 String이 Tsid Long으로 정상 디코딩 된다." {
            val cases =
                mapOf(
                    "0" to 0L,
                    "Z" to 61L,
                    "10" to 62L,
                    "1zA9" to 375_109L,
                )

            cases.forEach { (encoded, expected) ->
                TsidBase62Encoder.decode(encoded) shouldBe expected
            }
        }

        "encode 후 decode 하면 원래 값이 나온다" {
            val values = listOf(
                0L,
                1L,
                10L,
                61L,
                62L,
                999L,
                123456789L,
                Long.MAX_VALUE,
            )

            values.forEach { value ->
                val encoded = TsidBase62Encoder.encode(value)
                val decoded = TsidBase62Encoder.decode(encoded)

                decoded shouldBe value
            }
        }

        "유효하지 않은 Base62 문자가 들어오면 예외가 발생한다" {
            shouldThrow<IllegalArgumentException> {
                TsidBase62Encoder.decode("abc!")
            }
        }
    })
