package com.weeth.domain.cardinal.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class CardinalResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    CARDINAL_FIND_ALL_SUCCESS(11000, HttpStatus.OK, "전체 기수 조회에 성공했습니다."),
    CARDINAL_SAVE_SUCCESS(11001, HttpStatus.OK, "기수 저장에 성공했습니다."),
    CARDINAL_UPDATE_SUCCESS(11002, HttpStatus.OK, "기수 수정에 성공했습니다."),
}
