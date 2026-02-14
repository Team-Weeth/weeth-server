package leets.weeth.global.auth.apple.dto;

import lombok.Builder;

@Builder
public record AppleUserInfo(
        String appleId,
        String email,
        Boolean emailVerified
) {
}
