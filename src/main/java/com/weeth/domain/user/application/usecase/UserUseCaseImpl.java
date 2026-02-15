package com.weeth.domain.user.application.usecase;

import com.weeth.domain.user.application.dto.response.UserCardinalDto;
import com.weeth.domain.user.application.exception.PasswordMismatchException;
import com.weeth.domain.user.application.exception.StudentIdExistsException;
import com.weeth.domain.user.application.exception.TelExistsException;
import com.weeth.domain.user.application.exception.UserInActiveException;
import com.weeth.domain.user.application.mapper.CardinalMapper;
import com.weeth.domain.user.application.mapper.UserMapper;
import com.weeth.domain.user.domain.entity.Cardinal;
import com.weeth.domain.user.domain.entity.User;
import com.weeth.domain.user.domain.entity.UserCardinal;
import com.weeth.domain.user.domain.service.*;
import com.weeth.global.auth.apple.dto.AppleTokenResponse;
import com.weeth.global.auth.apple.dto.AppleUserInfo;
import com.weeth.global.auth.jwt.application.dto.JwtDto;
import com.weeth.global.auth.jwt.application.usecase.JwtManageUseCase;
import com.weeth.global.auth.kakao.KakaoAuthService;
import com.weeth.global.auth.kakao.dto.KakaoTokenResponse;
import com.weeth.global.auth.kakao.dto.KakaoUserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.weeth.domain.user.application.dto.request.UserRequestDto.*;
import static com.weeth.domain.user.application.dto.response.UserResponseDto.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserUseCaseImpl implements UserUseCase {
    private static final String BEARER = "Bearer ";
    private final JwtManageUseCase jwtManageUseCase;
    private final UserSaveService userSaveService;
    private final UserGetService userGetService;
    private final UserUpdateService userUpdateService;
    private final KakaoAuthService kakaoAuthService;
    private final com.weeth.global.auth.apple.AppleAuthService appleAuthService;
    private final CardinalGetService cardinalGetService;
    private final UserCardinalSaveService userCardinalSaveService;
    private final UserCardinalGetService userCardinalGetService;

    private final UserMapper mapper;
    private final CardinalMapper cardinalMapper;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Override
    @Transactional(readOnly = true)
    public SocialLoginResponse login(Login dto) {
        long kakaoId = getKakaoId(dto);
        Optional<User> optionalUser = userGetService.find(kakaoId);

        if (optionalUser.isEmpty()) {
            return mapper.toIntegrateResponse(kakaoId);
        }

        User user = optionalUser.get();
        if (user.isInactive()) {
            throw new UserInActiveException();
        }

        JwtDto token = jwtManageUseCase.create(user.getId(), user.getEmail(), user.getRole());
        return mapper.toLoginResponse(user, token);
    }

    @Override
    public SocialAuthResponse authenticate(Login dto) {
        long kakaoId = getKakaoId(dto);

        return mapper.toSocialAuthResponse(kakaoId);
    }

    @Override
    @Transactional
    public SocialLoginResponse integrate(NormalLogin dto) {
        User user = userGetService.find(dto.email());

        if (!passwordEncoder.matches(dto.passWord(), user.getPassword())) {
            throw new PasswordMismatchException();
        }
        user.addKakaoId(dto.kakaoId());

        if (user.isInactive()) {
            throw new UserInActiveException();
        }

        JwtDto token = jwtManageUseCase.create(user.getId(), user.getEmail(), user.getRole());

        return mapper.toLoginResponse(user, token);
    }

    @Override
    public Slice<SummaryResponse> findAllUser(int pageNumber, int pageSize, Integer cardinal) {

        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Slice<User> users;

        if (cardinal == null) {
            users = userGetService.findAll(pageable);

        } else {
            Cardinal inputCardinal = cardinalGetService.findByUserSide(cardinal);
            users = userGetService.findAll(pageable, inputCardinal);
        }

        List<UserCardinal> allUserCardinals = userCardinalGetService.findAll(users.getContent());

        Map<Long, List<UserCardinal>> userCardinalMap = allUserCardinals.stream()
                .collect(Collectors.groupingBy(userCardinal -> userCardinal.getUser().getId()));

        return users.map(user -> {
            List<UserCardinal> userCardinals = userCardinalMap.getOrDefault(user.getId(), Collections.emptyList());

            return mapper.toSummaryResponse(user, userCardinals);
        });
    }

    @Override
    public UserResponse findUserDetails(Long userId) {
        UserCardinalDto dto = getUserCardinalDto(userId);

        return mapper.toUserResponse(dto.user(), dto.cardinals());
    }

    @Override
    public Response find(Long userId) {
        UserCardinalDto dto = getUserCardinalDto(userId);

        return mapper.to(dto.user(), dto.cardinals());
    }

    @Override
    public void update(Update dto, Long userId) {
        validate(dto, userId);
        User user = userGetService.find(userId);
        userUpdateService.update(user, dto);
    }

    @Override
    @Transactional
    public void apply(SignUp dto) {
        validate(dto);

        Cardinal cardinal = cardinalGetService.findByUserSide(dto.cardinal());
        User user = mapper.from(dto, passwordEncoder);
        UserCardinal userCardinal = new UserCardinal(user, cardinal);

        userSaveService.save(user);
        userCardinalSaveService.save(userCardinal);
    }

    @Override
    @Transactional
    public void socialRegister(Register dto) {
        validate(dto);

        Cardinal cardinal = cardinalGetService.findByUserSide(dto.cardinal());

        User user = mapper.from(dto);
        UserCardinal userCardinal = new UserCardinal(user, cardinal);

        userSaveService.save(user);
        userCardinalSaveService.save(userCardinal);
    }

    @Override
    @Transactional
    public JwtDto refresh(String refreshToken) {

        String requestToken = refreshToken.replace(BEARER, "");

        JwtDto token = jwtManageUseCase.reIssueToken(requestToken);

        log.info("RefreshToken 발급 완료: {}", token);
        return new JwtDto(token.accessToken(), token.refreshToken());
    }

    @Override
    public UserInfo findUserInfo(Long userId) {
        UserCardinalDto dto = getUserCardinalDto(userId);

        return mapper.toUserInfoDto(dto.user(), dto.cardinals());
    }

    @Override
    public List<SummaryResponse> searchUser(String keyword) {
        List<User> users = userGetService.search(keyword);

        return users.stream()
                .map(user -> {
                    List<UserCardinal> userCardinals = userCardinalGetService.getUserCardinals(user);
                    return mapper.toSummaryResponse(user, userCardinals);
                })
                .toList();
    }

    private long getKakaoId(Login dto) {
        KakaoTokenResponse tokenResponse = kakaoAuthService.getKakaoToken(dto.authCode());
        KakaoUserInfoResponse userInfo = kakaoAuthService.getUserInfo(tokenResponse.access_token());

        return userInfo.id();
    }

    private void validate(Update dto, Long userId) {
        if (userGetService.validateStudentId(dto.studentId(), userId))
            throw new StudentIdExistsException();
        if (userGetService.validateTel(dto.tel(), userId))
            throw new TelExistsException();
    }

    private void validate(SignUp dto) {
        if (userGetService.validateStudentId(dto.studentId()))
            throw new StudentIdExistsException();
        if (userGetService.validateTel(dto.tel()))
            throw new TelExistsException();
    }

    private void validate(Register dto) {
        if (userGetService.validateStudentId(dto.studentId())) {
            throw new StudentIdExistsException();
        }
        if (userGetService.validateTel(dto.tel())) {
            throw new TelExistsException();
        }
    }

    private UserCardinalDto getUserCardinalDto(Long userId) {
        User user = userGetService.find(userId);
        List<UserCardinal> userCardinals = userCardinalGetService.getUserCardinals(user);

        return cardinalMapper.toUserCardinalDto(user, userCardinals);
    }

    @Override
    @Transactional(readOnly = true)
    public SocialLoginResponse appleLogin(Login dto) {
        // Apple Token 요청 및 유저 정보 요청
        AppleTokenResponse tokenResponse = appleAuthService.getAppleToken(dto.authCode());
        AppleUserInfo userInfo = appleAuthService.verifyAndDecodeIdToken(tokenResponse.id_token());

        String appleIdToken = tokenResponse.id_token();
        String appleId = userInfo.appleId();

        Optional<User> optionalUser = userGetService.findByAppleId(appleId);

        //todo: 추후 애플 로그인 연동을 위해 appleIdToken을 반환
        // 애플 로그인 연동 API 요청시 appleIdToken을 함께 넣어주면 그때 디코딩해서 appleId를 추출
        if (optionalUser.isEmpty()) {
            return mapper.toAppleIntegrateResponse(appleIdToken);
        }

        User user = optionalUser.get();
        if (user.isInactive()) {
            throw new UserInActiveException();
        }

        JwtDto token = jwtManageUseCase.create(user.getId(), user.getEmail(), user.getRole());
        return mapper.toAppleLoginResponse(user, token);
    }

    @Override
    @Transactional
    public void appleRegister(Register dto) {
        validate(dto);

        // Apple authCode로 토큰 교환 후 ID Token 검증 및 사용자 정보 추출
        AppleTokenResponse tokenResponse = appleAuthService.getAppleToken(dto.appleAuthCode());
        AppleUserInfo appleUserInfo = appleAuthService.verifyAndDecodeIdToken(tokenResponse.id_token());

        Cardinal cardinal = cardinalGetService.findByUserSide(dto.cardinal());

        User user = mapper.from(dto);
        // Apple ID 설정
        user.addAppleId(appleUserInfo.appleId());

        UserCardinal userCardinal = new UserCardinal(user, cardinal);

        userSaveService.save(user);
        userCardinalSaveService.save(userCardinal);

        // dev 환경에서만 바로 ACTIVE 상태로 설정
        if (isDevEnvironment()) {
            log.info("dev 환경 감지: 사용자 자동 승인 처리 (userId: {})", user.getId());
            user.accept();
        }
    }

    /**
     * 현재 환경이 dev 프로파일인지 확인
     * @return dev 프로파일이 활성화되어 있으면 true
     */
    private boolean isDevEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("dev".equals(profile)) {
                return true;
            }
            if ("local".equals(profile)) {
                return true;
            }
        }
        return false;
    }
}
