package com.weeth.domain.account.presentation;

import com.weeth.global.common.response.ResponseCodeInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AccountResponseCode implements ResponseCodeInterface {
    // AccountAdminController 관련
    ACCOUNT_SAVE_SUCCESS(1100, HttpStatus.OK, "회비가 성공적으로 저장되었습니다."),

    // AccountController 관련
    ACCOUNT_FIND_SUCCESS(1101, HttpStatus.OK, "회비가 성공적으로 조회되었습니다."),

    // ReceiptAdminController 관련
    RECEIPT_SAVE_SUCCESS(1102, HttpStatus.OK, "영수증이 성공적으로 저장되었습니다."),
    RECEIPT_DELETE_SUCCESS(1103, HttpStatus.OK, "영수증이 성공적으로 삭제되었습니다."),
    RECEIPT_UPDATE_SUCCESS(1104, HttpStatus.OK, "영수증이 성공적으로 업데이트 되었습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;

    AccountResponseCode(int code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
