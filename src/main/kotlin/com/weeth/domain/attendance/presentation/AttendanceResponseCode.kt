package com.weeth.domain.attendance.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class AttendanceResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    // AttendanceAdminController 관련
    ATTENDANCE_UPDATED_SUCCESS(10200, HttpStatus.OK, "개별 출석 상태가 성공적으로 수정되었습니다."),
    ATTENDANCE_FIND_DETAIL_SUCCESS(10201, HttpStatus.OK, "모든 인원의 정기모임 출석 정보가 성공적으로 조회되었습니다."),

    // AttendanceController 관련
    ATTENDANCE_CHECKIN_SUCCESS(10202, HttpStatus.OK, "출석이 성공적으로 처리되었습니다."),
    ATTENDANCE_FIND_SUCCESS(10203, HttpStatus.OK, "사용자의 출석 정보가 성공적으로 조회되었습니다."),
    ATTENDANCE_FIND_ALL_SUCCESS(10204, HttpStatus.OK, "사용자의 상세 출석 정보가 성공적으로 조회되었습니다."),

    // QR 관련
    QR_TOKEN_GENERATE_SUCCESS(10205, HttpStatus.OK, "QR 코드가 성공적으로 생성되었습니다."),
    QR_STATUS_FIND_SUCCESS(10206, HttpStatus.OK, "QR 상태가 성공적으로 조회되었습니다."),
}
