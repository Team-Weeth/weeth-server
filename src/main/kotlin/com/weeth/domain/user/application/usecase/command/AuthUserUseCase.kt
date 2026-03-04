package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.global.auth.jwt.application.dto.JwtDto
import com.weeth.global.auth.jwt.application.service.JwtTokenExtractor
import com.weeth.global.auth.jwt.application.usecase.JwtManageUseCase
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthUserUseCase(
    private val userReader: UserReader,
    private val jwtManageUseCase: JwtManageUseCase,
    private val jwtTokenExtractor: JwtTokenExtractor,
) {
    @Transactional
    fun leave(userId: Long) {
        val user = userReader.getById(userId)
        user.leave()
    }

    fun refreshToken(httpServletRequest: HttpServletRequest): JwtDto {
        val refreshToken = jwtTokenExtractor.extractRefreshToken(httpServletRequest)
        return jwtManageUseCase.reIssueToken(refreshToken)
    }
}
