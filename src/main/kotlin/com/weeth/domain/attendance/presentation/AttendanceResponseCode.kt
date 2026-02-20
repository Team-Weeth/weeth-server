package com.weeth.domain.attendance.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class AttendanceResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    // AttendanceAdminController 관련
    ATTENDANCE_CLOSE_SUCCESS(1200, HttpStatus.OK, "출석이 성공적으로 마감되었습니다."),
    ATTENDANCE_UPDATED_SUCCESS(1201, HttpStatus.OK, "개별 출석 상태가 성공적으로 수정되었습니다."),
    ATTENDANCE_FIND_DETAIL_SUCCESS(1202, HttpStatus.OK, "모든 인원의 정기모임 출석 정보가 성공적으로 조회되었습니다."),
    MEETING_FIND_SUCCESS(1203, HttpStatus.OK, "기수별 정기모임 리스트를 성공적으로 조회했습니다."),

    // AttendanceController 관련
    ATTENDANCE_CHECKIN_SUCCESS(1204, HttpStatus.OK, "출석이 성공적으로 처리되었습니다."),
    ATTENDANCE_FIND_SUCCESS(1205, HttpStatus.OK, "사용자의 출석 정보가 성공적으로 조회되었습니다."),
    ATTENDANCE_FIND_ALL_SUCCESS(1206, HttpStatus.OK, "사용자의 상세 출석 정보가 성공적으로 조회되었습니다."),

    // SessionAdminController 관련
    SESSION_SAVE_SUCCESS(1207, HttpStatus.OK, "정기모임이 성공적으로 생성되었습니다."),
    SESSION_UPDATE_SUCCESS(1208, HttpStatus.OK, "정기모임이 성공적으로 수정되었습니다."),
    SESSION_DELETE_SUCCESS(1209, HttpStatus.OK, "정기모임이 성공적으로 삭제되었습니다."),

    // SessionController 관련
    SESSION_FIND_SUCCESS(1210, HttpStatus.OK, "정기모임이 성공적으로 조회되었습니다."),
}
