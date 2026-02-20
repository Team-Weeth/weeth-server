package com.weeth.global.common.exception

import com.weeth.global.auth.jwt.application.exception.JwtErrorCode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.BindException
import org.springframework.validation.FieldError

class CommonExceptionHandlerTest :
    DescribeSpec({
        val handler = CommonExceptionHandler()

        describe("handle(BaseException)") {
            it("ErrorCode 기반 응답으로 변환한다") {
                val ex = object : BaseException(JwtErrorCode.TOKEN_NOT_FOUND) {}

                val response = handler.handle(ex)

                response.statusCode.value() shouldBe 404
                response.body?.code shouldBe 2902
            }
        }

        describe("handle(BindException)") {
            it("필드 에러 목록을 CommonResponse로 반환한다") {
                val bindingResult = BeanPropertyBindingResult(Any(), "request")
                bindingResult.addError(
                    FieldError("request", "name", "", false, emptyArray(), emptyArray(), "must not be blank"),
                )
                val ex = BindException(bindingResult)

                val response = handler.handle(ex)

                response.statusCode.value() shouldBe 400
                response.body?.message shouldBe "bindException"
            }
        }
    })
