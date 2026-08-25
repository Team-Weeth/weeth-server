package com.weeth.domain.schedule.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class ScheduleResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    EVENT_SAVE_SUCCESS(10800, HttpStatus.OK, "일정이 성공적으로 생성되었습니다."),
    EVENT_UPDATE_SUCCESS(10801, HttpStatus.OK, "일정이 성공적으로 수정되었습니다."),
    EVENT_DELETE_SUCCESS(10802, HttpStatus.OK, "일정이 성공적으로 삭제되었습니다."),
    EVENT_FIND_SUCCESS(10803, HttpStatus.OK, "일정이 성공적으로 조회되었습니다."),
    SCHEDULE_MONTHLY_FIND_SUCCESS(10804, HttpStatus.OK, "월별 일정이 성공적으로 조회되었습니다."),
    SCHEDULE_YEARLY_FIND_SUCCESS(10805, HttpStatus.OK, "연도별 일정이 성공적으로 조회되었습니다."),
    SCHEDULE_DETAIL_FIND_SUCCESS(10806, HttpStatus.OK, "일정 상세가 성공적으로 조회되었습니다."),
}
