package com.weeth.domain.file.presentation;

import com.weeth.global.common.response.ResponseCodeInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum FileResponseCode implements ResponseCodeInterface {

    PRESIGNED_URL_GET_SUCCESS(1500, HttpStatus.OK, "Presigned Url 반환에 성공했습니다");

    private final int code;
    private final HttpStatus status;
    private final String message;

    FileResponseCode(int code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

}
