package com.weeth.domain.minigame.domain.vo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class WaxNicknameTest :
    StringSpec({
        "2~8자의 한글·영문·숫자는 직접 입력 닉네임으로 받는다" {
            listOf("새내기", "ab", "Player01", "왁뿌볼고수12").forEach { WaxNickname(it).value shouldBe it }
        }

        "추천 후보는 공백이 있거나 8자를 넘어도 그대로 받는다" {
            WaxNickname.CANDIDATES.forEach { WaxNickname(it).value shouldBe it }
            WaxNickname.CANDIDATES.any { it.contains(' ') } shouldBe true
        }

        "길이·문자 규칙을 벗어난 직접 입력은 거절한다" {
            listOf("가", "아홉글자닉네임이다", "공백 포함", "특수!", "ㄱㄴ", "", "emoji😀").forEach { raw ->
                shouldThrow<IllegalArgumentException> { WaxNickname(raw) }
            }
        }

        "무작위 추천은 후보 목록에서 고른다" {
            WaxNickname.random(Random(1)).value shouldBeIn WaxNickname.CANDIDATES
        }
    })
