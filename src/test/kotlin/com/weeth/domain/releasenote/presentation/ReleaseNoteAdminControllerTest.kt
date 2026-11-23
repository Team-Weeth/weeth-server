package com.weeth.domain.releasenote.presentation

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.weeth.domain.releasenote.application.dto.response.ReleaseNoteIdResponse
import com.weeth.domain.releasenote.application.exception.NotDeveloperException
import com.weeth.domain.releasenote.application.usecase.command.ManageReleaseNoteUseCase
import com.weeth.domain.releasenote.application.usecase.query.GetReleaseNoteAdminQueryService
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.exception.CommonExceptionHandler
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class ReleaseNoteAdminControllerTest :
    DescribeSpec({
        val manageReleaseNoteUseCase = mockk<ManageReleaseNoteUseCase>()
        val getReleaseNoteAdminQueryService = mockk<GetReleaseNoteAdminQueryService>()
        val controller = ReleaseNoteAdminController(manageReleaseNoteUseCase, getReleaseNoteAdminQueryService)
        val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        val mvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(CommonExceptionHandler())
                .setMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
                .setCustomArgumentResolvers(
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

        beforeTest { clearMocks(manageReleaseNoteUseCase, getReleaseNoteAdminQueryService) }

        describe("POST /api/v4/admin/release-notes") {
            it("정상 작성이면 성공 코드 11604와 생성 id를 반환한다") {
                every { manageReleaseNoteUseCase.create(10L, any()) } returns ReleaseNoteIdResponse(12L)

                mvc
                    .perform(
                        post("/api/v4/admin/release-notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"title":"제목","content":"<p>본문</p>","status":"DRAFT"}"""),
                    ).andExpect(status().isOk)
                    .andExpect(jsonPath("$.code").value(ReleaseNoteResponseCode.RELEASE_NOTE_CREATED_SUCCESS.code))
                    .andExpect(jsonPath("$.data.id").value(12))
            }

            it("제목이 공백뿐이면 400을 반환한다") {
                mvc
                    .perform(
                        post("/api/v4/admin/release-notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"title":"   ","content":"<p>본문</p>"}"""),
                    ).andExpect(status().isBadRequest)
            }

            it("DEVELOPER가 아니면 403과 코드 21601을 반환한다") {
                every { manageReleaseNoteUseCase.create(10L, any()) } throws NotDeveloperException()

                mvc
                    .perform(
                        post("/api/v4/admin/release-notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"title":"제목","content":"<p>본문</p>"}"""),
                    ).andExpect(status().isForbidden)
                    .andExpect(jsonPath("$.code").value(21601))
            }
        }

        describe("PATCH /api/v4/admin/release-notes/{id}/status") {
            it("상태 변경이면 성공 코드 11606을 반환한다") {
                justRun { manageReleaseNoteUseCase.updateStatus(10L, 12L, any()) }

                mvc
                    .perform(
                        patch("/api/v4/admin/release-notes/12/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""{"status":"PUBLISHED"}"""),
                    ).andExpect(status().isOk)
                    .andExpect(
                        jsonPath("$.code").value(ReleaseNoteResponseCode.RELEASE_NOTE_STATUS_UPDATED_SUCCESS.code),
                    )
            }
        }

        describe("DELETE /api/v4/admin/release-notes/{id}") {
            it("삭제 성공 코드 11607을 반환한다") {
                justRun { manageReleaseNoteUseCase.delete(10L, 12L) }

                mvc
                    .perform(delete("/api/v4/admin/release-notes/12"))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.code").value(ReleaseNoteResponseCode.RELEASE_NOTE_DELETED_SUCCESS.code))
            }
        }
    })
