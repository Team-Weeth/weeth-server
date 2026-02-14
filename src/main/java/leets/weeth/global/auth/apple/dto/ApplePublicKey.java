package leets.weeth.global.auth.apple.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ApplePublicKey(
        String kty,
        String kid,
        String use,
        String alg,
        String n,
        String e
) {
}
