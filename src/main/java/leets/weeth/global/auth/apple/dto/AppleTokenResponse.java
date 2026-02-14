package leets.weeth.global.auth.apple.dto;

public record AppleTokenResponse(
        String access_token,
        String token_type,
        Long expires_in,
        String refresh_token,
        String id_token
) {
}
