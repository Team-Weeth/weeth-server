package com.weeth.global.common.response;

import lombok.Getter;

@Getter
public class CommonResponse<T> {    // 응답 객체

    private int code;
    private String message;
    private T data;

    public static <T> CommonResponse<T> createSuccess(String message) {
        return new CommonResponse<>(200, message, null);
    }

    public static <T> CommonResponse<T> createSuccess(String message, T data) {     // 성공
        return new CommonResponse<>(200, message, data);
    }

    public static <T> CommonResponse<T> createFailure(int code, String message) {   // 실패
        return new CommonResponse<>(code, message, null);
    }

    public static <T> CommonResponse<T> createFailure(int code, String message, T data) {   // 실패
        return new CommonResponse<>(code, message, data);
    }

    public CommonResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

}
