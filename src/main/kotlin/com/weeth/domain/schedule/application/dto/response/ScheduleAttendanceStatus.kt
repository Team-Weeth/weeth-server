package com.weeth.domain.schedule.application.dto.response

enum class ScheduleAttendanceStatus {
    UPCOMING,   // 출석 예정
    OPEN,       // 지금 출석 가능
    COMPLETED,  // 출석 완료
    ABSENT,     // 결석
}
