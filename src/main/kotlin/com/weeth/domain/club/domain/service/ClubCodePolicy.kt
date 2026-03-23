package com.weeth.domain.club.domain.service

import com.weeth.domain.club.application.exception.InvalidClubCodeException
import java.util.UUID

/**
 * 동아리 초대 코드 생성 및 검증 정책.
 * 형식: UUID(36자)
 */
object ClubCodePolicy {
    fun generateCode(): String = UUID.randomUUID().toString()

    /**
     * 제공된 코드가 클럽의 초대 코드와 일치하는지 검증
     */
    fun validate(
        clubCode: String,
        providedCode: String,
    ) {
        if (!clubCode.equals(providedCode.trim(), ignoreCase = true)) {
            throw InvalidClubCodeException()
        }
    }
}
