package com.weeth.domain.releasenote.domain.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ReleaseNoteSummaryExtractorTest :
    StringSpec({
        "HTML 태그를 제거하고 순수 텍스트만 남긴다" {
            ReleaseNoteSummaryExtractor.extract("<h3>개선된 기능</h3><p>본문 내용입니다.</p>") shouldBe "개선된 기능 본문 내용입니다."
        }

        "HTML 엔티티를 디코드한다" {
            ReleaseNoteSummaryExtractor.extract("<p>A&nbsp;&amp;&nbsp;B</p>") shouldBe "A & B"
        }

        "연속 공백을 하나로 합치고 앞뒤 공백을 제거한다" {
            ReleaseNoteSummaryExtractor.extract("<p>  여러   공백  </p>") shouldBe "여러 공백"
        }

        "100자를 초과하면 앞 100자만 남긴다" {
            val content = "<p>" + "가".repeat(200) + "</p>"

            val summary = ReleaseNoteSummaryExtractor.extract(content)

            summary.length shouldBe ReleaseNoteSummaryExtractor.MAX_SUMMARY_LENGTH
            summary shouldBe "가".repeat(ReleaseNoteSummaryExtractor.MAX_SUMMARY_LENGTH)
        }

        "script/style 블록은 내용까지 제거한다" {
            ReleaseNoteSummaryExtractor.extract(
                "<style>.a{color:red}</style><p>본문</p><script>alert(1)</script>",
            ) shouldBe
                "본문"
        }
    })
