package com.weeth.global.common.markdown

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class MarkdownToTiptapHtmlConverterTest :
    DescribeSpec({
        val converter = MarkdownToTiptapHtmlConverter()

        // 블록 사이 개행은 렌더 결과에 영향이 없으므로 비교 전에 걷어낸다
        fun convert(content: String) = converter.convert(content).replace("\n", "")

        describe("블록 서식 변환") {
            it("#/##/### 제목을 h1/h2/h3으로 변환한다") {
                convert("# 제목1\n\n## 제목2\n\n### 제목3") shouldBe "<h1>제목1</h1><h2>제목2</h2><h3>제목3</h3>"
            }

            it("불릿 리스트의 li 내부를 p로 감싼다") {
                convert("- 항목 1\n- 항목 2") shouldBe "<ul><li><p>항목 1</p></li><li><p>항목 2</p></li></ul>"
            }

            it("번호 리스트의 li 내부를 p로 감싼다") {
                convert("1. 첫째\n2. 둘째") shouldBe "<ol><li><p>첫째</p></li><li><p>둘째</p></li></ol>"
            }

            it("중첩 리스트도 각 항목을 p로 감싼다") {
                convert("- 항목\n  - 중첩") shouldBe "<ul><li><p>항목</p><ul><li><p>중첩</p></li></ul></li></ul>"
            }

            it("체크리스트를 taskList 구조로 변환한다") {
                convert("- [ ] 미완료\n- [x] 완료") shouldBe
                    "<ul data-type=\"taskList\">" +
                    "<li data-type=\"taskItem\" data-checked=\"false\"><p>미완료</p></li>" +
                    "<li data-type=\"taskItem\" data-checked=\"true\"><p>완료</p></li>" +
                    "</ul>"
            }

            it("인용을 blockquote 안의 p로 변환한다") {
                convert("> 인용문") shouldBe "<blockquote><p>인용문</p></blockquote>"
            }

            it("---를 hr로 변환한다") {
                convert("위\n\n---\n\n아래") shouldBe "<p>위</p><hr><p>아래</p>"
            }

            it("코드 블록을 pre > code로 변환하고 내부 줄바꿈과 언어를 보존한다") {
                val converted = converter.convert("```kotlin\nval a = 1\n\nval b = 2\n```")

                converted shouldContain "<pre><code class=\"language-kotlin\">val a = 1\n\nval b = 2\n</code></pre>"
            }

            it("테이블을 tbody 단일 구조로 변환하고 모든 셀에 colspan/rowspan을 채운다") {
                convert("| 헤더 1 | 헤더 2 |\n| --- | ---: |\n| 데이터 1 | 데이터 2 |") shouldBe
                    "<table><tbody>" +
                    "<tr><th colspan=\"1\" rowspan=\"1\"><p>헤더 1</p></th>" +
                    "<th colspan=\"1\" rowspan=\"1\"><p>헤더 2</p></th></tr>" +
                    "<tr><td colspan=\"1\" rowspan=\"1\"><p>데이터 1</p></td>" +
                    "<td colspan=\"1\" rowspan=\"1\"><p>데이터 2</p></td></tr>" +
                    "</tbody></table>"
            }
        }

        describe("인라인 서식 변환") {
            it("굵게/기울임/취소선/인라인 코드를 각각 strong/em/s/code로 변환한다") {
                convert("**굵게** _기울임_ ~~취소선~~ `코드`") shouldBe
                    "<p><strong>굵게</strong> <em>기울임</em> <s>취소선</s> <code>코드</code></p>"
            }

            it("마크다운 링크를 a로 변환한다") {
                convert("[링크](https://weeth.com)") shouldBe "<p><a href=\"https://weeth.com\">링크</a></p>"
            }

            it("링크의 target/rel은 프론트 Link 확장이 부여하므로 붙이지 않는다") {
                val converted = converter.convert("[링크](https://weeth.com)")

                converted shouldNotContain "target="
                converted shouldNotContain "rel="
            }
        }

        describe("줄바꿈 보존") {
            it("문단 내 단일 줄바꿈을 br로 보존한다") {
                convert("첫째 줄\n둘째 줄") shouldBe "<p>첫째 줄<br>둘째 줄</p>"
            }

            it("빈 줄이 연달아 있으면 빈 문단으로 간격을 보존한다") {
                convert("위\n\n\n\n아래") shouldBe "<p>위</p><p></p><p></p><p>아래</p>"
            }

            it("빈 줄 하나는 문단 구분으로만 쓴다") {
                convert("위\n\n아래") shouldBe "<p>위</p><p>아래</p>"
            }

            it("코드 블록 안의 빈 줄은 빈 문단으로 바꾸지 않는다") {
                converter.convert("```\na\n\nb\n```") shouldNotContain "<p></p>"
            }
        }

        describe("이관 데이터 처리") {
            it("p 한 덩어리에 갇힌 마크다운을 br 기준으로 복원해 변환한다") {
                convert("<p>## 제목입니다<br>- 항목 1<br>- 항목 2</p>") shouldBe
                    "<h2>제목입니다</h2><ul><li><p>항목 1</p></li><li><p>항목 2</p></li></ul>"
            }

            it("여러 p로 쪼개진 본문의 마크다운도 변환한다") {
                convert("<p>첫 문단</p><p>**굵은** 둘째 문단</p>") shouldBe
                    "<p>첫 문단</p><p><strong>굵은</strong> 둘째 문단</p>"
            }

            it("p 블록과 평문이 섞여 있어도 빈 문단을 덧붙이지 않는다") {
                convert("<p>첫 문단</p>\n\n이어지는 평문") shouldBe "<p>첫 문단</p><p>이어지는 평문</p>"
            }

            it("서식 없는 평문도 문단으로 감싼다") {
                convert("그냥 평범한 텍스트") shouldBe "<p>그냥 평범한 텍스트</p>"
            }

            it("빈 본문은 빈 문단으로 변환한다") {
                converter.convert("   ") shouldBe "<p></p>"
            }
        }

        describe("Tiptap 확장 제약 대응") {
            it("등록되지 않은 h4 이상 제목을 굵은 문단으로 강등한다") {
                convert("#### 제목4\n\n##### 제목5") shouldBe
                    "<p><strong>제목4</strong></p><p><strong>제목5</strong></p>"
            }

            it("마크다운 이미지를 Tiptap 이미지 태그로 유지한다") {
                convert("![대체텍스트](https://img.com/a.png)") shouldBe
                    "<p><img src=\"https://img.com/a.png\" alt=\"대체텍스트\"></p>"
            }
        }

        describe("변환 대상 판별") {
            it("이미 Tiptap HTML인 본문은 변환 대상이 아니다") {
                converter.needsConversion("<h2>제목</h2><ul><li><p>항목</p></li></ul>") shouldBe false
            }

            it("p로 래핑돼 있어도 안에 마크다운 문법이 남아 있으면 변환 대상이다") {
                converter.needsConversion("<p>## 제목입니다</p>") shouldBe true
            }

            it("코드 블록 안의 마크다운 유사 문자열은 변환 대상 판별에서 제외한다") {
                converter.needsConversion("<p>설명</p><pre><code># 주석\n- 항목</code></pre>") shouldBe false
            }

            it("서식 없는 평문은 문단 구조가 없으므로 변환 대상이다") {
                converter.needsConversion("그냥 평범한 텍스트") shouldBe true
            }
        }

        describe("기존 HTML 본문 보존") {
            it("이미 코드 블록인 내용은 마크다운으로 재해석하지 않는다") {
                val converted =
                    converter.convert("<p>- 대시로 시작하는 문장</p><pre><code># 주석\n- 항목</code></pre>")

                converted shouldContain "<pre><code># 주석\n- 항목\n</code></pre>"
                converted shouldNotContain "<h1>"
            }

            it("코드 블록의 언어 정보를 유지한다") {
                val converted =
                    converter.convert(
                        "<p>**설명**</p><pre><code class=\"language-kotlin\">val a = 1</code></pre>",
                    )

                converted shouldContain "<pre><code class=\"language-kotlin\">val a = 1\n</code></pre>"
            }

            it("테이블 열 너비(colwidth)를 살균 과정에서 지우지 않는다") {
                val converted =
                    converter.convert(
                        "<table><tbody><tr><th colspan=\"1\" rowspan=\"1\" colwidth=\"250\">" +
                            "<p>헤더</p></th></tr></tbody></table>",
                    )

                converted shouldContain "colwidth=\"250\""
            }
        }

        describe("안전성") {
            it("script 태그와 이벤트 핸들러 속성을 제거한다") {
                val converted = converter.convert("<script>alert(1)</script><p onclick=\"evil()\">텍스트</p>")

                converted shouldNotContain "script"
                converted shouldNotContain "onclick"
                converted shouldContain "<p>텍스트</p>"
            }

            it("javascript: 스킴 링크의 href를 제거한다") {
                converter.convert("[클릭](javascript:alert(1))") shouldNotContain "javascript:"
            }
        }

        describe("멱등성") {
            it("변환 결과를 다시 변환해도 달라지지 않는다") {
                val samples =
                    listOf(
                        "# 제목\n\n본문 첫 줄\n본문 둘째 줄\n\n\n- 항목 1\n- [x] 완료",
                        "> 인용\n\n```kotlin\nval a = 1\n```\n\n| A | B |\n| --- | --- |\n| 1 | 2 |",
                        "<p>## 제목<br>내용</p>",
                        "위\n\n\n\n아래",
                    )

                samples.forEach { sample ->
                    val once = converter.convert(sample)

                    converter.convert(once) shouldBe once
                }
            }
        }
    })
