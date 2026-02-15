package com.weeth.domain.account.presentation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {
    // AccountAdminController 관련
    ACCOUNT_SAVE_SUCCESS("회비가 성공적으로 저장되었습니다."),

    // AccountController 관련
    ACCOUNT_FIND_SUCCESS("회비가 성공적으로 조회되었습니다."),

    // ReceiptAdminController 관련
    RECEIPT_SAVE_SUCCESS("영수증이 성공적으로 저장되었습니다."),
    RECEIPT_DELETE_SUCCESS("영수증이 성공적으로 삭제되었습니다."),
    RECEIPT_UPDATE_SUCCESS("영수증이 성공적으로 업데이트 되었습니다.");

    private final String message;
}
