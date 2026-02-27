package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.user.application.dto.request.SignUpRequest
import com.weeth.domain.user.application.dto.request.SocialLoginRequest
import com.weeth.domain.user.application.dto.request.UpdateUserProfileRequest
import com.weeth.domain.user.application.dto.response.SocialLoginResponse
import com.weeth.domain.user.application.exception.EmailNotFoundException
import com.weeth.domain.user.application.exception.StudentIdExistsException
import com.weeth.domain.user.application.exception.TelExistsException
import com.weeth.domain.user.application.exception.UserInActiveException
import com.weeth.domain.user.application.mapper.UserMapper
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.UserCardinal
import com.weeth.domain.user.domain.entity.UserSocialAccount
import com.weeth.domain.user.domain.entity.enums.SocialProvider
import com.weeth.domain.user.domain.entity.enums.Status
import com.weeth.domain.user.domain.repository.CardinalReader
import com.weeth.domain.user.domain.repository.UserCardinalRepository
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.domain.user.domain.repository.UserSocialAccountRepository
import com.weeth.global.auth.apple.AppleAuthService
import com.weeth.global.auth.jwt.application.dto.JwtDto
import com.weeth.global.auth.jwt.application.service.JwtTokenExtractor
import com.weeth.global.auth.jwt.application.usecase.JwtManageUseCase
import com.weeth.global.auth.kakao.KakaoAuthService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthUserUseCase(
    private val userRepository: UserRepository,
    private val userReader: UserReader,
    private val cardinalReader: CardinalReader,
    private val userCardinalRepository: UserCardinalRepository,
    private val mapper: UserMapper,
    private val userSocialAccountRepository: UserSocialAccountRepository,
    private val kakaoAuthService: KakaoAuthService,
    private val appleAuthService: AppleAuthService,
    private val jwtManageUseCase: JwtManageUseCase,
    private val jwtTokenExtractor: JwtTokenExtractor,
) {
    @Transactional
    fun updateProfile(
        request: UpdateUserProfileRequest,
        userId: Long,
    ) {
        validate(request, userId)
        val user = userReader.getById(userId)
        user.update(request.name, request.email, request.studentId, request.tel, request.department)
    }

    @Transactional
    fun apply(request: SignUpRequest) { // todo: 리팩토링
        validate(request)
        val cardinal = cardinalReader.getByCardinalNumber(request.cardinal)
        val user = mapper.toEntity(request)
        val userCardinal = UserCardinal(user, cardinal)

        userRepository.save(user)
        userCardinalRepository.save(userCardinal)
    }

    @Transactional
    fun leave(userId: Long) {
        val user = userReader.getById(userId)
        user.leave()
    }

    @Transactional
    fun socialLoginByKakao(request: SocialLoginRequest): SocialLoginResponse { // todo: 리팩토링
        val kakaoToken = kakaoAuthService.getKakaoToken(request.authCode)
        val userInfo = kakaoAuthService.getUserInfo(kakaoToken.accessToken)
        val account = userInfo.kakaoAccount
        val email = account.email?.trim()?.lowercase()
        val providerName =
            account.profile
                ?.nickname
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        if (!account.isEmailValid || !account.isEmailVerified || email.isNullOrBlank()) {
            throw EmailNotFoundException()
        }
        return loginOrCreate(
            provider = SocialProvider.KAKAO,
            providerUserId = userInfo.id.toString(),
            providerEmail = email,
            providerName = providerName,
            request = request,
        )
    }

    @Transactional
    fun socialLoginByApple(request: SocialLoginRequest): SocialLoginResponse { // todo: 리팩토링
        val appleToken = appleAuthService.getAppleToken(request.authCode)
        val userInfo = appleAuthService.verifyAndDecodeIdToken(appleToken.idToken)
        val email = userInfo.email?.trim()?.lowercase()
        val providerName = userInfo.name?.trim()?.takeIf { it.isNotBlank() }
        if (!userInfo.emailVerified || email.isNullOrBlank()) {
            throw EmailNotFoundException()
        }
        return loginOrCreate(
            provider = SocialProvider.APPLE,
            providerUserId = userInfo.appleId,
            providerEmail = email,
            providerName = providerName,
            request = request,
        )
    }

    fun refreshToken(httpServletRequest: HttpServletRequest): JwtDto {
        val refreshToken = jwtTokenExtractor.extractRefreshToken(httpServletRequest)
        return jwtManageUseCase.reIssueToken(refreshToken)
    }

    private fun validate(
        request: UpdateUserProfileRequest,
        userId: Long,
    ) {
        if (userRepository.existsByStudentIdAndIdIsNot(request.studentId, userId)) {
            throw StudentIdExistsException()
        }
        if (userRepository.existsByTelAndIdIsNotValue(request.tel, userId)) {
            throw TelExistsException()
        }
    }

    private fun validate(request: SignUpRequest) {
        if (userRepository.existsByStudentId(request.studentId)) {
            throw StudentIdExistsException()
        }
        if (userRepository.existsByTelValue(request.tel)) {
            throw TelExistsException()
        }
    }

    private fun loginOrCreate(
        provider: SocialProvider,
        providerUserId: String,
        providerEmail: String,
        providerName: String?,
        request: SocialLoginRequest,
    ): SocialLoginResponse {
        val socialAccount = userSocialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId).orElse(null)
        val (user, isNewUser) =
            if (socialAccount != null) {
                socialAccount.user to false
            } else {
                createAndPersistSocialAccount(provider, providerUserId, providerEmail, providerName)
            }

        if (user.status == Status.BANNED || user.status == Status.LEFT) {
            throw UserInActiveException()
        }

        val hasExplicitPayload =
            request.name != null ||
                request.studentId != null ||
                request.tel != null ||
                request.department != null

        if (isNewUser || hasExplicitPayload) {
            applyOptionalProfile(user, request, providerName)
        }

        val token = jwtManageUseCase.create(user.id, user.emailValue, user.role)
        return SocialLoginResponse(
            email = user.emailValue,
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
            isNewUser = isNewUser,
            profileCompleted = user.isProfileCompleted(),
        )
    }

    private fun createAndPersistSocialAccount(
        provider: SocialProvider,
        providerUserId: String,
        providerEmail: String,
        providerName: String?,
    ): Pair<User, Boolean> {
        val existingUser = userRepository.findByEmailValue(providerEmail).orElse(null)
        val user =
            existingUser ?: userRepository.save(
                User.create(
                    name = providerName ?: "",
                    email = providerEmail,
                    studentId = "",
                    tel = "",
                    department = "",
                ),
            )
        userSocialAccountRepository.save(UserSocialAccount(provider = provider, providerUserId = providerUserId, user = user))
        return user to (existingUser == null)
    }

    private fun applyOptionalProfile(
        user: User,
        request: SocialLoginRequest,
        providerName: String?,
    ) {
        val hasProfilePayload =
            providerName != null ||
                request.name != null ||
                request.studentId != null ||
                request.tel != null ||
                request.department != null
        if (!hasProfilePayload) {
            return
        }

        val nextName = request.name?.trim()?.takeIf { it.isNotBlank() } ?: providerName ?: user.name
        val nextStudentId = request.studentId ?: user.studentId
        val nextTel = request.tel ?: user.telValue
        val nextDepartment = request.department ?: user.department

        if (
            nextStudentId != user.studentId &&
            nextStudentId.isNotBlank() &&
            userRepository.existsByStudentIdAndIdIsNot(nextStudentId, user.id)
        ) {
            throw StudentIdExistsException()
        }
        if (nextTel != user.telValue && nextTel.isNotBlank() && userRepository.existsByTelAndIdIsNotValue(nextTel, user.id)) {
            throw TelExistsException()
        }

        user.update(nextName, user.emailValue, nextStudentId, nextTel, nextDepartment)
    }
}
