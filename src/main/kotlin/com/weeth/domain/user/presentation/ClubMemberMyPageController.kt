package com.weeth.domain.user.presentation

import com.weeth.domain.club.application.exception.ClubErrorCode
import com.weeth.domain.user.application.dto.response.UserAttendedSessionResponse
import com.weeth.domain.user.application.dto.response.UserMyPageResponse
import com.weeth.domain.user.application.dto.response.UserMyPostResponse
import com.weeth.domain.user.application.exception.UserErrorCode
import com.weeth.domain.user.application.usecase.query.GetUserAttendanceQueryService
import com.weeth.domain.user.application.usecase.query.GetUserMyPageQueryService
import com.weeth.domain.user.application.usecase.query.GetUserPostQueryService
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.auth.jwt.application.exception.JwtErrorCode
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.response.SliceResponse
import com.weeth.global.common.web.TsidParam
import com.weeth.global.common.web.TsidPathVariable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "USER", description = "사용자 API")
@RestController
@RequestMapping("/api/v4/clubs/{clubId}/users/me/mypage")
@ApiErrorCodeExample(UserErrorCode::class, ClubErrorCode::class, JwtErrorCode::class)
class ClubMemberMyPageController(
    private val getUserPostQueryService: GetUserPostQueryService,
    private val getUserAttendanceQueryService: GetUserAttendanceQueryService,
    private val getUserMyPageQueryService: GetUserMyPageQueryService,
) {
    @GetMapping
    @Operation(summary = "현재 동아리 마이페이지 요약 조회")
    fun getSummary(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<UserMyPageResponse> {
        val response = getUserMyPageQueryService.getMyPage(userId, clubId)
        return CommonResponse.success(UserResponseCode.USER_MY_PAGE_FIND_SUCCESS, response)
    }

    @GetMapping("/posts")
    @Operation(summary = "현재 동아리에서 내가 쓴 글 조회")
    fun getMyPosts(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @RequestParam(defaultValue = "0") pageNumber: Int,
        @RequestParam(defaultValue = "5") pageSize: Int,
    ): CommonResponse<SliceResponse<UserMyPostResponse>> {
        val response = getUserPostQueryService.getMyPosts(userId, clubId, pageNumber, pageSize)
        return CommonResponse.success(UserResponseCode.USER_MY_POSTS_FIND_SUCCESS, response)
    }

    @GetMapping("/attended-sessions")
    @Operation(summary = "현재 동아리에서 출석한 세션 조회")
    fun getAttendedSessions(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @RequestParam(defaultValue = "0") pageNumber: Int,
        @RequestParam(defaultValue = "5") pageSize: Int,
    ): CommonResponse<SliceResponse<UserAttendedSessionResponse>> {
        val response = getUserAttendanceQueryService.getAttendedSessions(userId, clubId, pageNumber, pageSize)
        return CommonResponse.success(UserResponseCode.USER_ATTENDED_SESSIONS_FIND_SUCCESS, response)
    }
}
