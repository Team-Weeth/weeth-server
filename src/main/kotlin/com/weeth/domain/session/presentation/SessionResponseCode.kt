package com.weeth.domain.session.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class SessionResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    // SessionAdminController 관련
    SESSION_INFOS_FIND_SUCCESS(10300, HttpStatus.OK, "기수별 정기모임 리스트를 성공적으로 조회했습니다."),
    SESSION_SAVE_SUCCESS(10301, HttpStatus.OK, "정기모임이 성공적으로 생성되었습니다."),
    SESSION_UPDATE_SUCCESS(10302, HttpStatus.OK, "정기모임이 성공적으로 수정되었습니다."),
    SESSION_DELETE_SUCCESS(10303, HttpStatus.OK, "정기모임이 성공적으로 삭제되었습니다."),

    // SessionController 관련
    SESSION_FIND_SUCCESS(10304, HttpStatus.OK, "정기모임이 성공적으로 조회되었습니다."),
}
