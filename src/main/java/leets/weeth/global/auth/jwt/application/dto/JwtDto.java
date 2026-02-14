package leets.weeth.global.auth.jwt.application.dto;

public record JwtDto(
    String accessToken,
    String refreshToken
) {
}
