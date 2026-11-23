package com.weeth.domain.releasenote.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.weeth.domain.releasenote.application.dto.response.ReleaseNoteSummaryResponse
import com.weeth.domain.releasenote.application.exception.ReleaseNoteNotFoundException
import com.weeth.domain.releasenote.application.usecase.query.GetReleaseNoteQueryService
import com.weeth.domain.releasenote.domain.enums.ReleaseNoteStatus
import com.weeth.global.common.exception.CommonExceptionHandler
import com.weeth.global.common.response.SliceResponse
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ReleaseNoteControllerTest :
    DescribeSpec({
        val getReleaseNoteQueryService = mockk<GetReleaseNoteQueryService>()
        val controller = ReleaseNoteController(getReleaseNoteQueryService)
        val objectMapper = ObjectMapper().registerModule(JavaTimeModule())
        val mvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(CommonExceptionHandler())
                .setMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
                .build()

        beforeTest { clearMocks(getReleaseNoteQueryService) }

        describe("GET /api/v4/release-notes") {
            it("발행 노트 목록과 성공 코드 11600을 반환한다") {
                val summary =
                    ReleaseNoteSummaryResponse(
                        id = 12L,
                        title = "게시판 이미지 표시 개선",
                        summary = "이미지가 중복으로 보이지 않아요.",
                        thumbnailUrl = null,
                        status = ReleaseNoteStatus.PUBLISHED,
                    )
                every { getReleaseNoteQueryService.findAll(any(), any(), any()) } returns
                    SliceResponse(listOf(summary), 0, 12, 1, false)

                mvc
                    .perform(get("/api/v4/release-notes"))
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.code").value(ReleaseNoteResponseCode.RELEASE_NOTE_LIST_SUCCESS.code))
                    .andExpect(jsonPath("$.data.content[0].id").value(12))
                    .andExpect(jsonPath("$.data.content[0].summary").value("이미지가 중복으로 보이지 않아요."))
            }
        }

        describe("GET /api/v4/release-notes/{id}") {
            it("DRAFT이거나 없는 노트면 404와 코드 21600을 반환한다") {
                every { getReleaseNoteQueryService.find(99L) } throws ReleaseNoteNotFoundException()

                mvc
                    .perform(get("/api/v4/release-notes/99"))
                    .andExpect(status().isNotFound)
                    .andExpect(jsonPath("$.code").value(21600))
            }
        }
    })
