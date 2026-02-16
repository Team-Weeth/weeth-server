package com.weeth.global.auth.apple.dto;

import java.util.List;

public record ApplePublicKeys(
        List<ApplePublicKey> keys
) {
}
