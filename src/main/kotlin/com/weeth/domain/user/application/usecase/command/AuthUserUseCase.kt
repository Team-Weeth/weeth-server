package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.user.application.exception.UserInActiveException
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.global.auth.jwt.application.dto.JwtDto
import com.weeth.global.auth.jwt.application.exception.InvalidTokenException
import com.weeth.global.auth.jwt.application.service.JwtTokenExtractor
import com.weeth.global.auth.jwt.application.usecase.JwtManageUseCase
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service

@Service
class AuthUserUseCase(
    private val userReader: UserReader,
    private val jwtManageUseCase: JwtManageUseCase,
    private val jwtTokenExtractor: JwtTokenExtractor,
) {
    fun refreshToken(httpServletRequest: HttpServletRequest): JwtDto {
        val refreshToken = jwtTokenExtractor.extractRefreshToken(httpServletRequest)
        val userId = jwtTokenExtractor.extractId(refreshToken) ?: throw InvalidTokenException()
        val user = userReader.getById(userId)
        if (user.isBannedOrLeft()) throw UserInActiveException()
        return jwtManageUseCase.reIssueToken(refreshToken)
    }
}
