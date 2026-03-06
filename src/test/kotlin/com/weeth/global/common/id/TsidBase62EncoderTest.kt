package com.weeth.global.common.id

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
    })
