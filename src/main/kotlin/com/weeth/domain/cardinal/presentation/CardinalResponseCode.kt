package com.weeth.domain.cardinal.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class CardinalResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    CARDINAL_FIND_ALL_SUCCESS(1850, HttpStatus.OK, "전체 기수 조회에 성공했습니다."),
    CARDINAL_SAVE_SUCCESS(1851, HttpStatus.OK, "기수 저장에 성공했습니다."),
    CARDINAL_UPDATE_SUCCESS(1852, HttpStatus.OK, "기수 수정에 성공했습니다."),
}
