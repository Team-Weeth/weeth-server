package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.user.application.dto.request.SignUpRequest
import com.weeth.domain.user.application.dto.request.SocialLoginRequest
import com.weeth.domain.user.application.dto.request.UpdateUserProfileRequest
import com.weeth.domain.user.application.exception.StudentIdExistsException
import com.weeth.domain.user.application.exception.UserInActiveException
import com.weeth.domain.user.application.mapper.UserMapper
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.UserCardinal
import com.weeth.domain.user.domain.entity.enums.Status
import com.weeth.domain.user.domain.repository.CardinalReader
import com.weeth.domain.user.domain.repository.UserCardinalRepository
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.domain.repository.UserRepository
import com.weeth.domain.user.domain.repository.UserSocialAccountRepository
import com.weeth.domain.user.fixture.CardinalTestFixture
import com.weeth.domain.user.fixture.UserTestFixture
import com.weeth.global.auth.apple.AppleAuthService
import com.weeth.global.auth.apple.dto.AppleTokenResponse
import com.weeth.global.auth.apple.dto.AppleUserInfo
import com.weeth.global.auth.jwt.application.dto.JwtDto
import com.weeth.global.auth.jwt.application.service.JwtTokenExtractor
import com.weeth.global.auth.jwt.application.usecase.JwtManageUseCase
import com.weeth.global.auth.kakao.KakaoAuthService
import com.weeth.global.auth.kakao.dto.KakaoAccount
import com.weeth.global.auth.kakao.dto.KakaoProfile
import com.weeth.global.auth.kakao.dto.KakaoTokenResponse
import com.weeth.global.auth.kakao.dto.KakaoUserInfoResponse
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import java.util.Optional

class AuthUserUseCaseTest :
    DescribeSpec({
        val userRepository = mockk<UserRepository>(relaxed = true)
        val userReader = mockk<UserReader>()
        val cardinalReader = mockk<CardinalReader>()
        val userCardinalRepository = mockk<UserCardinalRepository>(relaxed = true)
        val userSocialAccountRepository = mockk<UserSocialAccountRepository>(relaxed = true)
        val mapper = mockk<UserMapper>()
        val kakaoAuthService = mockk<KakaoAuthService>()
        val appleAuthService = mockk<AppleAuthService>()
        val jwtManageUseCase = mockk<JwtManageUseCase>()
        val jwtTokenExtractor = mockk<JwtTokenExtractor>()

        val useCase =
            AuthUserUseCase(
                userRepository,
                userReader,
                cardinalReader,
                userCardinalRepository,
                mapper,
                userSocialAccountRepository,
                kakaoAuthService,
                appleAuthService,
                jwtManageUseCase,
                jwtTokenExtractor,
            )

        describe("apply") {
            it("유저와 유저-기수 연관관계를 저장한다") {
                val request = SignUpRequest("홍길동", "a@test.com", "20201234", "01012345678", "컴퓨터공학과", 7)
                val user = UserTestFixture.createActiveUser1(1L)
                val cardinal = CardinalTestFixture.createCardinal(id = 10L, cardinalNumber = 7, year = 2025, semester = 1)

                every { userRepository.existsByStudentId(request.studentId) } returns false
                every { userRepository.existsByTelValue(request.tel) } returns false
                every { cardinalReader.getByCardinalNumber(request.cardinal) } returns cardinal
                every { mapper.toEntity(request) } returns user
                every { userRepository.save(user) } returns user
                every { userCardinalRepository.save(any<UserCardinal>()) } answers { firstArg() }

                useCase.apply(request)

                verify(exactly = 1) { userRepository.save(user) }
                verify(exactly = 1) { userCardinalRepository.save(any<UserCardinal>()) }
            }

            it("학번 중복이면 StudentIdExistsException") {
                val request = SignUpRequest("홍길동", "a@test.com", "20201234", "01012345678", "컴퓨터공학과", 7)
                every { userRepository.existsByStudentId(request.studentId) } returns true

                shouldThrow<StudentIdExistsException> {
                    useCase.apply(request)
                }
            }
        }

        describe("updateProfile") {
            it("내 정보를 수정한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                val request = UpdateUserProfileRequest("변경이름", "new@test.com", "20209999", "01099998888", "경영학과")

                every { userRepository.existsByStudentIdAndIdIsNot(request.studentId, 1L) } returns false
                every { userRepository.existsByTelAndIdIsNotValue(request.tel, 1L) } returns false
                every { userReader.getById(1L) } returns user

                useCase.updateProfile(request, 1L)

                user.name shouldBe "변경이름"
                user.department shouldBe "경영학과"
            }
        }

        describe("leave") {
            it("회원 탈퇴 시 상태를 LEFT로 변경한다") {
                val user = UserTestFixture.createActiveUser1(1L)
                every { userReader.getById(1L) } returns user

                useCase.leave(1L)

                user.status shouldBe Status.LEFT
            }
        }

        describe("socialLoginByKakao") {
            it("가입된 활성 사용자면 토큰을 발급한다") {
                val request = SocialLoginRequest("auth-code")
                val tokenResponse = KakaoTokenResponse("bearer", "kakao-access", 3600, "kakao-refresh", 3600)
                val userInfo =
                    KakaoUserInfoResponse(
                        id = 1L,
                        kakaoAccount = KakaoAccount(isEmailValid = true, isEmailVerified = true, email = "a@test.com"),
                    )
                val user = UserTestFixture.createActiveUser1(1L)

                every { kakaoAuthService.getKakaoToken("auth-code") } returns tokenResponse
                every { kakaoAuthService.getUserInfo("kakao-access") } returns userInfo
                every { userSocialAccountRepository.findByProviderAndProviderUserId(any(), any()) } returns Optional.empty()
                every { userRepository.findByEmailValue("a@test.com") } returns Optional.of(user)
                every { userSocialAccountRepository.save(any()) } answers { firstArg() }
                every { jwtManageUseCase.create(user.id, user.emailValue, user.role) } returns JwtDto("access", "refresh")

                val result = useCase.socialLoginByKakao(request)

                result.isNewUser shouldBe false
                result.profileCompleted shouldBe false
                result.accessToken shouldBe "access"
                result.refreshToken shouldBe "refresh"
            }

            it("기존 사용자가 추가 프로필 payload 없이 로그인하면 provider 이름으로 덮어쓰지 않는다") {
                val request = SocialLoginRequest("auth-code")
                val tokenResponse = KakaoTokenResponse("bearer", "kakao-access", 3600, "kakao-refresh", 3600)
                val userInfo =
                    KakaoUserInfoResponse(
                        id = 1L,
                        kakaoAccount =
                            KakaoAccount(
                                isEmailValid = true,
                                isEmailVerified = true,
                                email = "a@test.com",
                                profile = KakaoProfile(nickname = "카카오닉네임"),
                            ),
                    )
                val user = UserTestFixture.createActiveUser1(1L).also { it.name = "내가수정한이름" }

                every { kakaoAuthService.getKakaoToken("auth-code") } returns tokenResponse
                every { kakaoAuthService.getUserInfo("kakao-access") } returns userInfo
                every { userSocialAccountRepository.findByProviderAndProviderUserId(any(), any()) } returns Optional.empty()
                every { userRepository.findByEmailValue("a@test.com") } returns Optional.of(user)
                every { userSocialAccountRepository.save(any()) } answers { firstArg() }
                every { jwtManageUseCase.create(user.id, user.emailValue, user.role) } returns JwtDto("access", "refresh")

                useCase.socialLoginByKakao(request)

                user.name shouldBe "내가수정한이름"
            }

            it("식별자가 없고 이메일 사용자도 없으면 사용자를 생성하고 로그인한다") {
                val request = SocialLoginRequest("auth-code")
                val tokenResponse = KakaoTokenResponse("bearer", "kakao-access", 3600, "kakao-refresh", 3600)
                val userInfo =
                    KakaoUserInfoResponse(
                        id = 1L,
                        kakaoAccount = KakaoAccount(isEmailValid = true, isEmailVerified = true, email = "new@test.com"),
                    )
                val createdUser = User.create(name = "", email = "new@test.com", studentId = "", tel = "", department = "")

                every { kakaoAuthService.getKakaoToken("auth-code") } returns tokenResponse
                every { kakaoAuthService.getUserInfo("kakao-access") } returns userInfo
                every { userSocialAccountRepository.findByProviderAndProviderUserId(any(), any()) } returns Optional.empty()
                every { userRepository.findByEmailValue("new@test.com") } returns Optional.empty()
                every { userRepository.save(any()) } returns createdUser
                every { userSocialAccountRepository.save(any()) } answers { firstArg() }
                every {
                    jwtManageUseCase.create(
                        createdUser.id,
                        createdUser.emailValue,
                        createdUser.role,
                    )
                } returns
                    JwtDto(
                        "access",
                        "refresh",
                    )

                val result = useCase.socialLoginByKakao(request)

                result.isNewUser shouldBe true
                result.email shouldBe "new@test.com"
                result.accessToken shouldBe "access"
            }

            it("추방된 사용자면 예외를 던진다") {
                val request = SocialLoginRequest("auth-code")
                val tokenResponse = KakaoTokenResponse("bearer", "kakao-access", 3600, "kakao-refresh", 3600)
                val userInfo =
                    KakaoUserInfoResponse(
                        id = 1L,
                        kakaoAccount = KakaoAccount(isEmailValid = true, isEmailVerified = true, email = "ban@test.com"),
                    )
                val bannedUser = UserTestFixture.createActiveUser1(1L).also { it.ban() }

                every { kakaoAuthService.getKakaoToken("auth-code") } returns tokenResponse
                every { kakaoAuthService.getUserInfo("kakao-access") } returns userInfo
                every { userSocialAccountRepository.findByProviderAndProviderUserId(any(), any()) } returns Optional.empty()
                every { userRepository.findByEmailValue("ban@test.com") } returns Optional.of(bannedUser)
                every { userSocialAccountRepository.save(any()) } answers { firstArg() }

                shouldThrow<UserInActiveException> {
                    useCase.socialLoginByKakao(request)
                }
            }
        }

        describe("socialLoginByApple") {
            it("가입된 활성 사용자면 토큰을 발급한다") {
                val request = SocialLoginRequest("apple-code")
                val tokenResponse = AppleTokenResponse("apple-access", "bearer", 3600, "apple-refresh", "id-token")
                val userInfo = AppleUserInfo(appleId = "apple-sub", email = "apple@test.com", emailVerified = true)
                val user = UserTestFixture.createActiveUser1(1L)

                every { appleAuthService.getAppleToken("apple-code") } returns tokenResponse
                every { appleAuthService.verifyAndDecodeIdToken("id-token") } returns userInfo
                every { userSocialAccountRepository.findByProviderAndProviderUserId(any(), any()) } returns Optional.empty()
                every { userRepository.findByEmailValue("apple@test.com") } returns Optional.of(user)
                every { userSocialAccountRepository.save(any()) } answers { firstArg() }
                every { jwtManageUseCase.create(user.id, user.emailValue, user.role) } returns JwtDto("access", "refresh")

                val result = useCase.socialLoginByApple(request)

                result.isNewUser shouldBe false
                result.accessToken shouldBe "access"
            }
        }

        describe("refreshToken") {
            it("헤더의 리프레시 토큰으로 토큰을 재발급한다") {
                val servletRequest = mockk<HttpServletRequest>()
                every { jwtTokenExtractor.extractRefreshToken(servletRequest) } returns "refresh-token"
                every { jwtManageUseCase.reIssueToken("refresh-token") } returns JwtDto("new-access", "new-refresh")

                val result = useCase.refreshToken(servletRequest)

                result.accessToken shouldBe "new-access"
                result.refreshToken shouldBe "new-refresh"
            }
        }
    })
