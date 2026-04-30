package com.weeth.domain.dashboard.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class DashboardResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    DASHBOARD_HOME_SUCCESS(11200, HttpStatus.OK, "홈 정보를 성공적으로 조회했습니다."),
    DASHBOARD_RECENT_POSTS_SUCCESS(11201, HttpStatus.OK, "최신 게시글을 성공적으로 조회했습니다."),
    DASHBOARD_RECENT_NOTICES_SUCCESS(11202, HttpStatus.OK, "최신 공지를 성공적으로 조회했습니다."),
    DASHBOARD_MONTHLY_SCHEDULES_SUCCESS(11203, HttpStatus.OK, "월간 일정을 성공적으로 조회했습니다."),
    DASHBOARD_UNREAD_NOTICE_SUCCESS(11204, HttpStatus.OK, "읽지 않은 공지를 성공적으로 조회했습니다."),
}
