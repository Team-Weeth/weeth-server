package com.weeth.domain.dashboard.application.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class DashboardHomeResponse(
    @field:Schema(description = "현재 동아리 정보")
    val club: DashboardClubInfoResponse,
    @field:Schema(description = "오늘의 일정")
    val todaySchedules: List<DashboardScheduleResponse>,
    @field:Schema(description = "가입한 동아리 목록")
    val myClubs: List<DashboardMyClubResponse>,
)
