package com.weeth.domain.penalty.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class PenaltyResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    PENALTY_ASSIGN_SUCCESS(1600, HttpStatus.OK, "페널티가 성공적으로 부여되었습니다."),
    PENALTY_FIND_ALL_SUCCESS(1601, HttpStatus.OK, "모든 패널티가 성공적으로 조회되었습니다."),
    PENALTY_DELETE_SUCCESS(1602, HttpStatus.OK, "패널티가 성공적으로 삭제되었습니다."),
    PENALTY_UPDATE_SUCCESS(1603, HttpStatus.OK, "패널티를 성공적으로 수정했습니다."),
    PENALTY_USER_FIND_SUCCESS(1604, HttpStatus.OK, "패널티가 성공적으로 조회되었습니다."),
}
