package com.weeth.domain.comment.domain.vo

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CommentContentTest :
    StringSpec({
        "정상 내용이면 생성된다" {
            val content = CommentContent.from("정상 내용")

            content.value shouldBe "정상 내용"
        }

        "빈 문자열이면 예외를 던진다" {
            shouldThrow<IllegalArgumentException> {
                CommentContent.from("")
            }
        }

        "공백만 있으면 예외를 던진다" {
            shouldThrow<IllegalArgumentException> {
                CommentContent.from("   ")
            }
        }

        "300자는 허용된다" {
            val content = CommentContent.from("a".repeat(300))

            content.value.length shouldBe 300
        }

        "301자이면 예외를 던진다" {
            shouldThrow<IllegalArgumentException> {
                CommentContent.from("a".repeat(301))
            }
        }
    })
