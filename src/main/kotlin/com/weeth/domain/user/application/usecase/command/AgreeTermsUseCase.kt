package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.user.application.dto.request.AgreeTermsRequest
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.global.auth.jwt.application.dto.JwtDto
import com.weeth.global.auth.jwt.application.usecase.JwtManageUseCase
import com.weeth.global.auth.jwt.domain.enums.TokenType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AgreeTermsUseCase(
    private val userRepository: UserRepository,
    private val jwtManageUseCase: JwtManageUseCase,
) {
    @Transactional
    fun execute(
        userId: Long,
        request: AgreeTermsRequest,
    ): JwtDto {
        val user = userRepository.getById(userId)
        user.agreeTerms(request.termsAgreed, request.privacyAgreed)
        user.accept() // 약관 동의시 회원가입 승인

        return jwtManageUseCase.create(userId, user.emailValue, TokenType.ACCESS)
    }
}
