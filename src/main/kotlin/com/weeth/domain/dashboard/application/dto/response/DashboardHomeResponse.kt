package com.weeth.domain.dashboard.application.dto.response

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema

data class DashboardHomeResponse(
    @field:Schema(description = "현재 동아리 정보")
    val club: DashboardClubInfoResponse,
    @field:Schema(description = "내 활동 정보")
    val myInfo: DashboardMyInfoResponse,
    @field:Schema(description = "회비 기능 부원 공개 여부. false면 프론트에서 회비 탭을 숨긴다.", example = "true")
    val accountVisible: Boolean,
    // MVP 제외 (이후 개발 시 @field:Schema(description = "오늘의 일정") 추가)
    @JsonIgnore
    val todaySchedules: List<DashboardScheduleResponse>,
    // MVP 제외 (이후 개발 시 @field:Schema(description = "가입한 동아리 목록") 추가)
    @JsonIgnore
    val myClubs: List<DashboardMyClubResponse>,
)
