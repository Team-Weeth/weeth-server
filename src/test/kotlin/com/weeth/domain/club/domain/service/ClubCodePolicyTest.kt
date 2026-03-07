package com.weeth.domain.club.domain.service

import com.weeth.domain.club.application.exception.InvalidClubCodeException
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveLength
import java.util.UUID

class ClubCodePolicyTest :
    StringSpec({
        "초대 코드는 UUID로 생성되어야 한다" {
            val code = ClubCodePolicy.generateCode()
            code shouldHaveLength 36

            shouldNotThrow<IllegalArgumentException> {
                UUID.fromString(code)
            }
        }

        "매번 생성되는 코드는 달라야 한다" {
            val code1 = ClubCodePolicy.generateCode()
            val code2 = ClubCodePolicy.generateCode()
            code1 shouldNotBe code2
        }

        "초대 코드가 일치하면 검증 성공" {
            val code = ClubCodePolicy.generateCode()

            shouldNotThrow<InvalidClubCodeException> {
                ClubCodePolicy.validate(code, code)
            }
        }

        "초대 코드가 일치하지 않으면 예외 발생" {
            val clubCode = ClubCodePolicy.generateCode()
            val providedCode = ClubCodePolicy.generateCode()

            shouldThrow<InvalidClubCodeException> {
                ClubCodePolicy.validate(clubCode, providedCode)
            }
        }

        "초대 코드는 대소문자가 달라도 검증 성공" {
            val clubCode = "ABCDEF12-3456-7890-ABCD-EF1234567890"
            val providedCode = "abcdef12-3456-7890-abcd-ef1234567890"

            shouldNotThrow<InvalidClubCodeException> {
                ClubCodePolicy.validate(clubCode, providedCode)
            }
        }

        "초대 코드는 앞뒤 공백을 제거한 뒤 검증 성공" {
            val code = ClubCodePolicy.generateCode().uppercase()
            val providedCode = "  $code  "

            shouldNotThrow<InvalidClubCodeException> {
                ClubCodePolicy.validate(code, providedCode)
            }
        }
    })
