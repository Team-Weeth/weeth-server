package com.weeth.domain.user.application.dto.response;

import com.weeth.domain.user.domain.entity.enums.LoginStatus;
import com.weeth.domain.user.domain.entity.enums.Position;
import com.weeth.domain.user.domain.entity.enums.Role;
import com.weeth.domain.user.domain.entity.enums.Status;

import java.time.LocalDateTime;
import java.util.List;

public class UserResponseDto {

    public record SocialLoginResponse(
            Long id,
            Long kakaoId,
            String appleIdToken,
            LoginStatus status,
            String accessToken,
            String refreshToken
    ) {
    }

    public record Response(
            Integer id,
            String name,
            String email,
            String studentId,
            String tel,
            String department,
            List<Integer> cardinals,
            Position position,
            Role role
    ) {
    }

    public record SummaryResponse(
            Integer id,
            String name,
            List<Integer> cardinals,
            Position position,
            Role role
    ) {
    }

    public record AdminResponse(
            Integer id,
            String name,
            String email,
            String studentId,
            String tel,
            String department,
            List<Integer> cardinals,
            Position position,
            Status status,
            Role role,
            Integer attendanceCount,
            Integer absenceCount,
            Integer attendanceRate,
            Integer penaltyCount,
            Integer warningCount,
            LocalDateTime createdAt,
            LocalDateTime modifiedAt
    ) {
    }

    public record UserResponse(
            Integer id,
            String name,
            String email,
            String studentId,
            String department,
            List<Integer> cardinals,
            Position position,
            Role role
    ) {
    }

    public record SocialAuthResponse(
            Long kakaoId
    ) {
    }

    public record UserInfo(
            Long id,
            String name,
            List<Integer> cardinals,
            Role role
    ) {
    }
} //todo: User 전역 dto 구현 (id, 이름, role)
