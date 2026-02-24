package com.weeth.domain.attendance.presentation

import com.weeth.domain.attendance.application.exception.SessionErrorCode
import com.weeth.domain.attendance.application.usecase.query.GetSessionQueryService
import com.weeth.domain.schedule.application.dto.response.SessionResponse
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "SESSION", description = "정기모임 API")
@RestController
@RequestMapping("/api/v4/sessions")
@ApiErrorCodeExample(SessionErrorCode::class)
class SessionController(
    private val getSessionQueryService: GetSessionQueryService,
) {
    @GetMapping("/{sessionId}")
    @Operation(summary = "정기모임 상세 조회")
    fun getSession(
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @PathVariable sessionId: Long,
    ): CommonResponse<SessionResponse> =
        CommonResponse.success(AttendanceResponseCode.SESSION_FIND_SUCCESS, getSessionQueryService.findSession(userId, sessionId))
}
