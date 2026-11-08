package com.weeth.domain.notification.presentation

import com.ninjasquad.springmockk.MockkBean
import com.weeth.domain.club.application.exception.MemberNotActiveException
import com.weeth.domain.notification.application.dto.response.NotificationCategoryPreferenceResponse
import com.weeth.domain.notification.application.dto.response.NotificationPreferencesResponse
import com.weeth.domain.notification.application.exception.InvalidNotificationPreferenceException
import com.weeth.domain.notification.application.exception.NotificationErrorCode
import com.weeth.domain.notification.application.usecase.command.ManageNotificationPreferenceUseCase
import com.weeth.domain.notification.application.usecase.query.GetNotificationPreferenceQueryService
import com.weeth.domain.notification.domain.enums.NotificationCategory
import com.weeth.global.auth.model.AuthenticatedUser
import com.weeth.global.common.exception.CommonExceptionHandler
import com.weeth.global.common.id.TsidBase62Encoder
import com.weeth.global.config.WebMvcConfig
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(NotificationPreferenceController::class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(
    classes = [NotificationPreferenceController::class, CommonExceptionHandler::class, WebMvcConfig::class],
)
class NotificationPreferenceControllerTest(
    private val mvc: MockMvc,
    @MockkBean private val query: GetNotificationPreferenceQueryService,
    @MockkBean private val useCase: ManageNotificationPreferenceUseCase,
) : DescribeSpec({
        val url = "/api/v4/clubs/${TsidBase62Encoder.encode(1L)}/notifications/preferences"
        beforeTest {
            clearMocks(query, useCase)
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(AuthenticatedUser(10L, "test@test.com"), null, emptyList())
        }
        afterTest { SecurityContextHolder.clearContext() }

        it("GET은 설정 데이터와 성공 코드를 반환한다") {
            every { query.find(1L, 10L) } returns
                NotificationPreferencesResponse(
                    true,
                    listOf(NotificationCategoryPreferenceResponse(NotificationCategory.NOTICE, true, true, true, true)),
                )
            mvc
                .perform(get(url))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(11406))
                .andExpect(jsonPath("$.data.masterEnabled").value(true))
                .andExpect(jsonPath("$.data.preferences[0].category").value("NOTICE"))
                .andExpect(jsonPath("$.data.preferences[0].effectiveEnabled").value(true))
            verify(exactly = 1) { query.find(1L, 10L) }
        }

        it("PATCH는 false를 보존하고 생략 필드를 null로 전달한다") {
            justRun { useCase.update(1L, 10L, any()) }
            mvc
                .perform(patch(url).contentType(MediaType.APPLICATION_JSON).content("""{"masterEnabled":false}"""))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(11407))
            verify { useCase.update(1L, 10L, match { it.masterEnabled == false && it.preferences == null }) }
        }

        it("명시적 null과 빈 PATCH는 변경 없음으로 전달한다") {
            justRun { useCase.update(1L, 10L, any()) }
            listOf("{}", """{"masterEnabled":null,"preferences":null}""").forEach { body ->
                mvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk)
            }
            verify(
                exactly = 2,
            ) { useCase.update(1L, 10L, match { it.masterEnabled == null && it.preferences == null }) }
        }

        it("미지원 카테고리와 불완전 항목은 400으로 거부한다") {
            listOf(
                """{"preferences":[{"category":"ATTENDANCE","enabled":false}]}""",
                """{"preferences":[{"category":"NOTICE"}]}""",
                """{"preferences":[{"enabled":false}]}""",
                """{"preferences":[null]}""",
            ).forEach { body ->
                mvc
                    .perform(
                        patch(url).contentType(MediaType.APPLICATION_JSON).content(body),
                    ).andExpect(status().isBadRequest)
            }
            verify(exactly = 0) { useCase.update(any(), any(), any()) }
        }

        it("유효한 카테고리 요청은 false를 보존해 전달한다") {
            justRun { useCase.update(1L, 10L, any()) }
            mvc
                .perform(
                    patch(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"preferences":[{"category":"SCHEDULE","enabled":false}]}"""),
                ).andExpect(status().isOk)
            verify {
                useCase.update(
                    1L,
                    10L,
                    match {
                        val item = it.preferences?.single()
                        it.masterEnabled == null && item?.category == NotificationCategory.SCHEDULE &&
                            item.enabled == false
                    },
                )
            }
        }

        it("설정 정책 오류는 notification 도메인 코드와 400으로 반환한다") {
            every { useCase.update(1L, 10L, any()) } throws
                InvalidNotificationPreferenceException(NotificationErrorCode.NOTIFICATION_MASTER_DISABLED)
            mvc
                .perform(
                    patch(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"preferences":[{"category":"NOTICE","enabled":false}]}"""),
                ).andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value(21402))
        }

        it("비활성 회원 오류를 응답하고 익명 요청은 인증 오류로 거부한다") {
            every { query.find(1L, 10L) } throws MemberNotActiveException()
            mvc.perform(get(url)).andExpect(status().isForbidden)
            SecurityContextHolder.clearContext()
            mvc.perform(get(url)).andExpect(status().isUnauthorized)
        }
    })
