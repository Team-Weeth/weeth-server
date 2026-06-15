package com.weeth.domain.user.application.usecase.command

import com.weeth.global.auth.jwt.application.dto.JwtDto
import com.weeth.global.auth.jwt.application.service.JwtTokenExtractor
import com.weeth.global.auth.jwt.application.usecase.JwtManageUseCase
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service

@Service
class AuthUserUseCase(
    private val jwtManageUseCase: JwtManageUseCase,
    private val jwtTokenExtractor: JwtTokenExtractor,
) {
    fun refreshToken(httpServletRequest: HttpServletRequest): JwtDto {
        val refreshToken = jwtTokenExtractor.extractRefreshToken(httpServletRequest)
        return jwtManageUseCase.reIssueToken(refreshToken)
    }
}
