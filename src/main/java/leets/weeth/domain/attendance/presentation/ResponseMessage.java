package leets.weeth.domain.attendance.presentation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {
    //AttendanceAdminController 관련
    ATTENDANCE_CLOSE_SUCCESS("출석이 성공적으로 마감되었습니다."),
    ATTENDANCE_UPDATED_SUCCESS("개별 출석 상태가 성공적으로 수정되었습니다."),
    ATTENDANCE_FIND_DETAIL_SUCCESS("모든 인원의 정기모임 출석 정보가 성공적으로 조회되었습니다."),
    MEETING_FIND_SUCCESS("기수별 정기모임 리스트를 성공적으로 조회했습니다."),

    //AttendanceController 관련
    ATTENDANCE_CHECKIN_SUCCESS("출석이 성공적으로 처리되었습니다."),
    ATTENDANCE_FIND_SUCCESS("사용자의 출석 정보가 성공적으로 조회되었습니다."),
    ATTENDANCE_FIND_ALL_SUCCESS("사용자의 상세 출석 정보가 성공적으로 조회되었습니다.");

    private final String message;
}
