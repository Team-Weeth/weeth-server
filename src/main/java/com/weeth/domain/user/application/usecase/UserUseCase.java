package com.weeth.domain.user.application.usecase;

import com.weeth.domain.user.application.dto.request.UserRequestDto;
import com.weeth.domain.user.application.dto.response.UserResponseDto;
import com.weeth.global.auth.jwt.application.dto.JwtDto;
import org.springframework.data.domain.Slice;

import java.util.List;

import static com.weeth.domain.user.application.dto.request.UserRequestDto.*;
import static com.weeth.domain.user.application.dto.response.UserResponseDto.*;


public interface UserUseCase {

    SocialLoginResponse login(Login dto);

    SocialAuthResponse authenticate(Login dto);

    SocialLoginResponse integrate(NormalLogin dto);

    UserResponseDto.Response find(Long userId);

    Slice<SummaryResponse> findAllUser(int pageNumber, int pageSize, Integer cardinal);

    UserResponseDto.UserResponse findUserDetails(Long userId);

    void update(UserRequestDto.Update dto, Long userId);

    void apply(SignUp dto);

    void socialRegister(Register dto);

    JwtDto refresh(String refreshToken);

    UserResponseDto.UserInfo findUserInfo(Long userId);

    List<SummaryResponse> searchUser(String keyword);

    SocialLoginResponse appleLogin(Login dto);

    void appleRegister(Register dto);

}
