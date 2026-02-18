package com.weeth.domain.penalty.presentation;

import com.weeth.global.common.response.ResponseCodeInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum PenaltyResponseCode implements ResponseCodeInterface {
    // penaltyAdminController 관련
    PENALTY_ASSIGN_SUCCESS(1600, HttpStatus.OK, "페널티가 성공적으로 부여되었습니다."),
    PENALTY_FIND_ALL_SUCCESS(1601, HttpStatus.OK, "모든 패널티가 성공적으로 조회되었습니다."),
    PENALTY_DELETE_SUCCESS(1602, HttpStatus.OK, "패널티가 성공적으로 삭제되었습니다."),
    PENALTY_UPDATE_SUCCESS(1603, HttpStatus.OK, "패널티를 성공적으로 수정했습니다."),
    // penaltyUserController
    PENALTY_USER_FIND_SUCCESS(1604, HttpStatus.OK, "패널티가 성공적으로 조회되었습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;

    PenaltyResponseCode(int code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
