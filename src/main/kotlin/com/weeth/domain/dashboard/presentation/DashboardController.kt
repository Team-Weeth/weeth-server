package com.weeth.domain.dashboard.presentation

import com.weeth.domain.club.application.exception.ClubErrorCode
import com.weeth.domain.dashboard.application.dto.response.DashboardHomeResponse
import com.weeth.domain.dashboard.application.dto.response.DashboardNoticeResponse
import com.weeth.domain.dashboard.application.dto.response.DashboardPostResponse
import com.weeth.domain.dashboard.application.dto.response.DashboardScheduleResponse
import com.weeth.domain.dashboard.application.dto.response.DashboardUnreadNoticeResponse
import com.weeth.domain.dashboard.application.exception.DashboardErrorCode
import com.weeth.domain.dashboard.application.usecase.query.GetDashboardQueryService
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.auth.jwt.application.exception.JwtErrorCode
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.web.TsidPathVariable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Slice
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "DASHBOARD", description = "대시보드 API")
@RestController
@RequestMapping("/api/v4/dashboard")
@ApiErrorCodeExample(DashboardErrorCode::class, ClubErrorCode::class, JwtErrorCode::class)
class DashboardController(
    private val getDashboardQueryService: GetDashboardQueryService,
) {
    @GetMapping("/{clubId}/home")
    @Operation(summary = "홈 조회")
    fun getHome(
        @TsidPathVariable("clubId") clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<DashboardHomeResponse> =
        CommonResponse.success(
            DashboardResponseCode.DASHBOARD_HOME_SUCCESS,
            getDashboardQueryService.getHome(clubId, userId),
        )

    @GetMapping("/{clubId}/recent-posts")
    @Operation(summary = "최신 게시글 조회")
    fun getRecentPosts(
        @TsidPathVariable("clubId") clubId: Long,
        @RequestParam(defaultValue = "0") pageNumber: Int,
        @RequestParam(defaultValue = "10") pageSize: Int,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Slice<DashboardPostResponse>> =
        CommonResponse.success(
            DashboardResponseCode.DASHBOARD_RECENT_POSTS_SUCCESS,
            getDashboardQueryService.getRecentPosts(clubId, userId, pageNumber, pageSize),
        )

    @GetMapping("/{clubId}/recent-notices")
    @Operation(summary = "최신 공지 조회")
    fun getRecentNotices(
        @TsidPathVariable("clubId") clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<List<DashboardNoticeResponse>> =
        CommonResponse.success(
            DashboardResponseCode.DASHBOARD_RECENT_NOTICES_SUCCESS,
            getDashboardQueryService.getRecentNotices(clubId, userId),
        )

    @GetMapping("/{clubId}/monthly-schedules")
    @Operation(summary = "월간 일정 조회")
    fun getMonthlySchedules(
        @TsidPathVariable("clubId") clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<List<DashboardScheduleResponse>> =
        CommonResponse.success(
            DashboardResponseCode.DASHBOARD_MONTHLY_SCHEDULES_SUCCESS,
            getDashboardQueryService.getMonthlySchedules(clubId, userId),
        )

    @GetMapping("/{clubId}/unread-notice")
    @Operation(summary = "2주 이내 읽지 않은 공지 조회")
    fun getUnreadNotice(
        @TsidPathVariable("clubId") clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<DashboardUnreadNoticeResponse?> =
        CommonResponse.success(
            DashboardResponseCode.DASHBOARD_UNREAD_NOTICE_SUCCESS,
            getDashboardQueryService.getUnreadNotice(clubId, userId),
        )
}
