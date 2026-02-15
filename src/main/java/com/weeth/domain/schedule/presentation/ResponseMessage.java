package com.weeth.domain.schedule.presentation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {
    // EventAdminController 관련
    EVENT_SAVE_SUCCESS("일정/정기모임이 성공적으로 생성되었습니다."),
    EVENT_UPDATE_SUCCESS("일정/정기모임이 성공적으로 수정되었습니다."),
    EVENT_DELETE_SUCCESS("일정이 성공적으로 삭제되었습니다."),
    // EventController 관련
    EVENT_FIND_SUCCESS("일정이 성공적으로 조회되었습니다."),
    // MeetingAdminController 관련
    MEETING_SAVE_SUCCESS("정기모임 일정이 성공적으로 생성되었습니다."),
    MEETING_UPDATE_SUCCESS("정기모임 일정이 성공적으로 수정되었습니다."),
    MEETING_DELETE_SUCCESS("정기모임 일정이 성공적으로 삭제되었습니다."),
    MEETING_CARDINAL_FIND_SUCCESS("특정 기수 정기모임이 성공적으로 조회되었습니다."),
    MEETING_ALL_FIND_SUCCESS("정기모임 전체일정이 성공적으로 조회되었습니다."),
    // MeetingController 관련
    MEETING_FIND_SUCCESS("정기모임이 성공적으로 조회되었습니다."),
    // ScheduleController 관련
    SCHEDULE_MONTHLY_FIND_SUCCESS("월별 일정이 성공적으로 조회되었습니다."),
    SCHEDULE_YEARLY_FIND_SUCCESS("연도별 일정이 성공적으로 조회되었습니다.");

    private final String message;
}
