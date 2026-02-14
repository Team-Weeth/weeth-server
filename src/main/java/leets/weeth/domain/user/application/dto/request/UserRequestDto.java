package leets.weeth.domain.user.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import leets.weeth.domain.user.domain.entity.enums.Role;

import java.util.List;

public class UserRequestDto {

    public record Login(
            @NotBlank String authCode
    ) {
    }

    public record SignUp(
            @NotBlank String name,
            @Email @NotBlank String email,
            @NotBlank String password,
            @NotBlank String studentId,
            @NotBlank String tel,
            @NotNull String position,
            @NotNull String department,
            @NotNull Integer cardinal
    ) {
    }

    public record Register(
            @Schema(description = "kakao로 회원가입 하는 경우")
            Long kakaoId,
            @Schema(description = "애플로 회원가입 하는 경우 - Apple OAuth authCode")
            String appleAuthCode,
            @NotBlank String name,
            @NotBlank String studentId,
            @NotBlank String email,
            @NotNull String department,
            @NotBlank String tel,
            @NotNull Integer cardinal,
            @NotNull String position
    ) {
    }

    public record Update(
            @NotBlank String name,
            @Email @NotBlank String email,
            @NotBlank String studentId,
            @NotBlank String tel,
            @NotNull String department
    ) {
    }

    public record NormalLogin(
            @Email @NotBlank String email,
            @NotBlank String passWord,
            @NotNull Long kakaoId
    ) {
    }

    public record UserRoleUpdate(
            @NotNull Long userId,
            @NotNull Role role
    ) {
    }

    public record UserApplyOB(
            @NotNull Long userId,
            @NotNull Integer cardinal
    ) {
    }

    public record UserId(
            @NotNull List<Long> userId
    ) {
    }

}
