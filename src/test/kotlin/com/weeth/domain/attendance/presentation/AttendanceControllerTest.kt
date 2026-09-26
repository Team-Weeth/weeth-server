package com.weeth.domain.attendance.presentation

import com.weeth.domain.attendance.application.dto.response.AttendanceDetailResponse
import com.weeth.domain.attendance.application.usecase.command.ManageAttendanceUseCase
import com.weeth.domain.attendance.application.usecase.command.SubscribeAttendanceSseUseCase
import com.weeth.domain.attendance.application.usecase.query.GetAttendanceQueryService
import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.exception.CommonExceptionHandler
import com.weeth.global.common.web.TsidPathVariableArgumentResolver
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.core.MethodParameter
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class AttendanceControllerTest :
    DescribeSpec({
        val service = mockk<GetAttendanceQueryService>()
        val controller =
            AttendanceController(mockk<ManageAttendanceUseCase>(), service, mockk<SubscribeAttendanceSseUseCase>())
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
        beforeTest { clearMocks(service) }
        it("상세의 기수 쿼리를 전달하고 기존 코드와 새 기수 통계를 직렬화한다") {
            every { service.findAllDetailsByCurrentCardinal(1L, 10L, 7) } returns
                AttendanceDetailResponse(1, 2, 1, emptyList(), 7, 50)
            mvc
                .perform(get("/api/v4/clubs/1/attendances/detail").param("cardinalNumber", "7"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code").value(AttendanceResponseCode.ATTENDANCE_FIND_ALL_SUCCESS.code))
                .andExpect(jsonPath("$.data.cardinalNumber").value(7))
                .andExpect(jsonPath("$.data.attendanceRate").value(50))
        }
        it("기수 생략 요청도 기존 경로로 처리한다") {
            every { service.findAllDetailsByCurrentCardinal(1L, 10L, null) } returns
                AttendanceDetailResponse(0, 0, 0, emptyList(), 8, 0)
            mvc
                .perform(get("/api/v4/clubs/1/attendances/detail"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.cardinalNumber").value(8))
        }
        it("없는 기수는 공통 응답 21000과 404이다") {
            every { service.findAllDetailsByCurrentCardinal(1L, 10L, 999) } throws CardinalNotFoundException()
            mvc
                .perform(get("/api/v4/clubs/1/attendances/detail").param("cardinalNumber", "999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value(21000))
        }
        it("문자열 기수는 400이다") {
            mvc
                .perform(get("/api/v4/clubs/1/attendances/detail").param("cardinalNumber", "abc"))
                .andExpect(status().isBadRequest)
        }
    })
