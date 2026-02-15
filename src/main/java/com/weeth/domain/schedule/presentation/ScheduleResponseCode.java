package com.weeth.domain.schedule.presentation;

import com.weeth.global.common.response.ResponseCodeInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ScheduleResponseCode implements ResponseCodeInterface {
    // EventAdminController 관련
    EVENT_SAVE_SUCCESS(1700, HttpStatus.OK, "일정/정기모임이 성공적으로 생성되었습니다."),
    EVENT_UPDATE_SUCCESS(1701, HttpStatus.OK, "일정/정기모임이 성공적으로 수정되었습니다."),
    EVENT_DELETE_SUCCESS(1702, HttpStatus.OK, "일정이 성공적으로 삭제되었습니다."),
    // EventController 관련
    EVENT_FIND_SUCCESS(1703, HttpStatus.OK, "일정이 성공적으로 조회되었습니다."),
    // MeetingAdminController 관련
    MEETING_SAVE_SUCCESS(1704, HttpStatus.OK, "정기모임 일정이 성공적으로 생성되었습니다."),
    MEETING_UPDATE_SUCCESS(1705, HttpStatus.OK, "정기모임 일정이 성공적으로 수정되었습니다."),
    MEETING_DELETE_SUCCESS(1706, HttpStatus.OK, "정기모임 일정이 성공적으로 삭제되었습니다."),
    MEETING_CARDINAL_FIND_SUCCESS(1707, HttpStatus.OK, "특정 기수 정기모임이 성공적으로 조회되었습니다."),
    MEETING_ALL_FIND_SUCCESS(1708, HttpStatus.OK, "정기모임 전체일정이 성공적으로 조회되었습니다."),
    // MeetingController 관련
    MEETING_FIND_SUCCESS(1709, HttpStatus.OK, "정기모임이 성공적으로 조회되었습니다."),
    // ScheduleController 관련
    SCHEDULE_MONTHLY_FIND_SUCCESS(1710, HttpStatus.OK, "월별 일정이 성공적으로 조회되었습니다."),
    SCHEDULE_YEARLY_FIND_SUCCESS(1711, HttpStatus.OK, "연도별 일정이 성공적으로 조회되었습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;

    ScheduleResponseCode(int code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
