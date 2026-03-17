package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.user.application.dto.request.SocialLoginRequest
import com.weeth.domain.user.application.dto.response.SocialLoginResponse
import com.weeth.domain.user.application.exception.EmailNotFoundException
import com.weeth.domain.user.application.exception.UserInActiveException
import com.weeth.domain.user.application.mapper.UserMapper
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.UserSocialAccount
import com.weeth.domain.user.domain.enums.SocialProvider
import com.weeth.domain.user.domain.enums.Status
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.domain.user.domain.repository.UserSocialAccountRepository
import com.weeth.domain.user.domain.vo.SocialAuthResult
import com.weeth.domain.user.infrastructure.SocialAuthPortRegistry
import com.weeth.global.auth.jwt.application.usecase.JwtManageUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SocialLoginUseCase(
    private val userRepository: UserRepository,
    private val userSocialAccountRepository: UserSocialAccountRepository,
    private val socialAuthPortRegistry: SocialAuthPortRegistry,
    private val jwtManageUseCase: JwtManageUseCase,
    private val userMapper: UserMapper,
) {
    @Transactional
    fun socialLoginByKakao(request: SocialLoginRequest): SocialLoginResponse =
        socialLogin(SocialProvider.KAKAO, request)

    @Transactional
    fun socialLoginByApple(request: SocialLoginRequest): SocialLoginResponse =
        socialLogin(SocialProvider.APPLE, request)

    private fun socialLogin(
        provider: SocialProvider,
        request: SocialLoginRequest,
    ): SocialLoginResponse {
        val authResult = socialAuthPortRegistry.get(provider).authenticate(request.authCode)
        val (user, isNewUser) = findOrCreateUser(authResult)

        if (user.isBannedOrLeft()) throw UserInActiveException()

        val token = jwtManageUseCase.create(user.id, user.emailValue)

        return userMapper.toSocialLoginResponse(token, isNewUser)
    }

    // TODO: 실제 서비스 출시 시 이메일 기반 기존 사용자 연동 및 유저 알림 기능 필요
    private fun findOrCreateUser(authResult: SocialAuthResult): Pair<User, Boolean> {
        val existing =
            userSocialAccountRepository
                .findByProviderAndProviderUserId(authResult.provider, authResult.providerUserId)
                .orElse(null)

        if (existing != null) return existing.user to false

        val email =
            authResult.email.takeIf { authResult.emailVerified && it.isNotBlank() } ?: throw EmailNotFoundException()

        val user =
            userRepository.save(
                User.create(
                    name = authResult.name ?: "",
                    email = email,
                    status = Status.ACTIVE, // 소셜 로그인으로 회원가입 한 경우 바로 가입 승인
                ),
            )

        userSocialAccountRepository.save(
            UserSocialAccount(
                provider = authResult.provider,
                providerUserId = authResult.providerUserId,
                user = user,
            ),
        )

        return user to true
    }
}
