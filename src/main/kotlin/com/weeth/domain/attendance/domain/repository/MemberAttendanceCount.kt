package com.weeth.domain.attendance.domain.repository

/** 페이지에 포함된 멤버의 선택 기수 출석 집계. 미결은 두 카운트 모두에서 제외한다. */
interface MemberAttendanceCount {
    val clubMemberId: Long
    val attendanceCount: Long
    val absenceCount: Long
}
