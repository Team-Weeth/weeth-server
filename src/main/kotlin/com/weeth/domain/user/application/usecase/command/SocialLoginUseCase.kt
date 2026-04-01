package com.weeth.domain.user.application.usecase.command

import com.fasterxml.jackson.databind.ObjectMapper
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
import com.weeth.global.auth.jwt.domain.enums.TokenType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SocialLoginUseCase(
    private val userRepository: UserRepository,
    private val userSocialAccountRepository: UserSocialAccountRepository,
    private val socialAuthPortRegistry: SocialAuthPortRegistry,
    private val jwtManageUseCase: JwtManageUseCase,
    private val userMapper: UserMapper,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun socialLoginByKakao(request: SocialLoginRequest): SocialLoginResponse =
        socialLogin(SocialProvider.KAKAO, request)

    @Transactional
    fun socialLoginByApple(request: SocialLoginRequest): SocialLoginResponse =
        socialLogin(SocialProvider.APPLE, request)

    /**
     * Apple form_post 콜백 전용 로그인.
     * id_token을 직접 검증하여 code 교환 과정을 생략하고,
     * Apple이 최초 인가 시에만 전달하는 user JSON의 이름을 반영한다.
     *
     * TODO: 탈퇴 기능 구현 시 Apple 계정 연결 해제(revoke)를 위해
     *  콜백의 code를 Apple 토큰 엔드포인트에 교환하여 refresh token을 받고 DB에 저장해야 한다.
     *  (Apple Revoke Tokens API: POST https://appleid.apple.com/auth/revoke)
     */
    @Transactional
    fun socialLoginByAppleCallback(
        idToken: String,
        userJson: String?,
    ): SocialLoginResponse {
        val authResult = socialAuthPortRegistry.get(SocialProvider.APPLE).authenticateWithIdToken(idToken)
        val userName = parseAppleUserName(userJson)
        val effectiveResult =
            if (!userName.isNullOrBlank() && authResult.name.isNullOrBlank()) {
                authResult.copy(name = userName)
            } else {
                authResult
            }
        return processLogin(effectiveResult)
    }

    private fun socialLogin(
        provider: SocialProvider,
        request: SocialLoginRequest,
    ): SocialLoginResponse = processLogin(socialAuthPortRegistry.get(provider).authenticate(request.authCode))

    private fun processLogin(authResult: SocialAuthResult): SocialLoginResponse {
        val user = findOrCreateUser(authResult)

        if (user.isBannedOrLeft()) throw UserInActiveException()

        val tokenType = if (user.isRegistered()) TokenType.ACCESS else TokenType.TEMPORARY
        val token = jwtManageUseCase.create(user.id, user.emailValue, tokenType)

        return userMapper.toSocialLoginResponse(user.name, token, user.isRegistered())
    }

    // TODO: 실제 서비스 출시 시 이메일 기반 기존 사용자 연동 및 유저 알림 기능 필요
    private fun findOrCreateUser(authResult: SocialAuthResult): User {
        val existing =
            userSocialAccountRepository
                .findByProviderAndProviderUserId(authResult.provider, authResult.providerUserId)
                .orElse(null)

        if (existing != null) return existing.user

        val email =
            authResult.email.takeIf { authResult.emailVerified && it.isNotBlank() } ?: throw EmailNotFoundException()

        val user =
            userRepository.save(
                User.create(
                    name = authResult.name?.takeIf { it.isNotBlank() } ?: email.substringBefore("@"),
                    email = email,
                    status = Status.WAITING, // 소셜 로그인으로 회원가입 한 경우 WAITING으로 초기화 -> 동의 완료시 ACTIVE
                ),
            )

        userSocialAccountRepository.save(
            UserSocialAccount(
                provider = authResult.provider,
                providerUserId = authResult.providerUserId,
                user = user,
            ),
        )

        return user
    }

    private fun parseAppleUserName(userJson: String?): String? {
        if (userJson.isNullOrBlank()) return null
        return try {
            val node = objectMapper.readTree(userJson)
            val nameNode = node["name"] ?: return null
            val firstName = nameNode["firstName"]?.asText()?.trim() ?: ""
            val lastName = nameNode["lastName"]?.asText()?.trim() ?: ""
            "$lastName$firstName".trim().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            log.warn("Apple user JSON 파싱 실패: {}", e.message)
            null
        }
    }
}
