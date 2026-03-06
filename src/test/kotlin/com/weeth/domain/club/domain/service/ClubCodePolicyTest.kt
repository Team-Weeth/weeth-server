package com.weeth.domain.club.domain.service

import com.weeth.domain.club.application.exception.InvalidClubCodeException
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.regex.shouldMatch
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
            assert(code1 != code2)
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
    })
