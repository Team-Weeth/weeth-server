package com.weeth.global.common.exception;

import lombok.Builder;

@Builder
public record BindExceptionResponse(
        String message,
        Object value
) {
}
