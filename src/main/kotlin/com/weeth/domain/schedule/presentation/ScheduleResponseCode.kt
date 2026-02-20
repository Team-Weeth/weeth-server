package com.weeth.domain.schedule.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class ScheduleResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    EVENT_SAVE_SUCCESS(1700, HttpStatus.OK, "일정이 성공적으로 생성되었습니다."),
    EVENT_UPDATE_SUCCESS(1701, HttpStatus.OK, "일정이 성공적으로 수정되었습니다."),
    EVENT_DELETE_SUCCESS(1702, HttpStatus.OK, "일정이 성공적으로 삭제되었습니다."),
    EVENT_FIND_SUCCESS(1703, HttpStatus.OK, "일정이 성공적으로 조회되었습니다."),
    SCHEDULE_MONTHLY_FIND_SUCCESS(1710, HttpStatus.OK, "월별 일정이 성공적으로 조회되었습니다."),
    SCHEDULE_YEARLY_FIND_SUCCESS(1711, HttpStatus.OK, "연도별 일정이 성공적으로 조회되었습니다."),
}
