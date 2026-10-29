package com.weeth.domain.feedback.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.weeth.domain.feedback.application.usecase.command.CreateFeedbackUseCase
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.exception.CommonExceptionHandler
import com.weeth.global.common.id.TsidBase62Encoder
import com.weeth.global.common.web.TsidPathVariableArgumentResolver
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.justRun
import io.mockk.mockk
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class FeedbackControllerTest :
    DescribeSpec({
        val createFeedbackUseCase = mockk<CreateFeedbackUseCase>()
        val controller = FeedbackController(createFeedbackUseCase)
        val objectMapper = ObjectMapper()
        val clubPathId = TsidBase62Encoder.encode(1L)
        val mvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(CommonExceptionHandler())
                .setCustomArgumentResolvers(
                    TsidPathVariableArgumentResolver(),
                    object : HandlerMethodArgumentResolver {
                        override fun supportsParameter(parameter: MethodParameter) =
                            parameter.hasParameterAnnotation(CurrentUser::class.java)

                        override fun resolveArgument(
                            parameter: MethodParameter,
                            mavContainer: ModelAndViewContainer?,
                            webRequest: NativeWebRequest,
                            binderFactory: WebDataBinderFactory?,
                        ): Any = 10L
                    },
                ).build()

        beforeTest { clearMocks(createFeedbackUseCase) }

        describe("POST /api/v4/clubs/{clubId}/feedbacks") {
            it("정상 요청이면 피드백 저장 성공 코드 11500을 반환한다") {
                justRun { createFeedbackUseCase.execute(1L, any(), 10L) }

                mvc
                    .perform(
                        post("/api/v4/clubs/$clubPathId/feedbacks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content("""{"content":"출석 체크 화면이 느려요.","channel":"CHAT"}"""),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.code").value(FeedbackResponseCode.FEEDBACK_CREATED_SUCCESS.code))
            }

            it("내용이 공백뿐이면 400을 반환한다") {
                mvc
                    .perform(
                        post("/api/v4/clubs/$clubPathId/feedbacks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content("""{"content":"   ","channel":"CHAT"}"""),
                    ).andExpect(status().isBadRequest)
            }

            it("내용이 1000자를 초과하면 400을 반환한다") {
                val tooLong = objectMapper.writeValueAsString("가".repeat(1001))
                mvc
                    .perform(
                        post("/api/v4/clubs/$clubPathId/feedbacks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content("""{"content":$tooLong,"channel":"CHAT"}"""),
                    ).andExpect(status().isBadRequest)
            }

            it("channel이 CHAT/FORM이 아니면 400을 반환한다") {
                mvc
                    .perform(
                        post("/api/v4/clubs/$clubPathId/feedbacks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content("""{"content":"내용입니다.","channel":"INVALID"}"""),
                    ).andExpect(status().isBadRequest)
            }
        }
    })
