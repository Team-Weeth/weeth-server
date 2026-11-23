package com.weeth.domain.minigame.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class MinigameResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    WAX_RANKING_FIND_SUCCESS(11600, HttpStatus.OK, "왁뿌볼 랭킹을 성공적으로 조회했습니다."),
}
