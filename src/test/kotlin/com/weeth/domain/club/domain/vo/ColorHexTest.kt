package com.weeth.domain.club.domain.vo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ColorHexTest :
    StringSpec({
        "정상적인 #RRGGBB 형식은 생성된다" {
            val colorHex = ColorHex("#4CAF50")

            colorHex.value shouldBe "#4CAF50"
        }

        "# 없이 시작하면 예외가 발생한다" {
            shouldThrow<IllegalArgumentException> {
                ColorHex("4CAF50")
            }
        }

        "6자리 hex가 아니면 예외가 발생한다" {
            shouldThrow<IllegalArgumentException> {
                ColorHex("#4CAF5")
            }
        }

        "hex가 아닌 문자가 포함되면 예외가 발생한다" {
            shouldThrow<IllegalArgumentException> {
                ColorHex("#GGGGGG")
            }
        }
    })
