package leets.weeth.domain.penalty.presentation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {
    // penaltyAdminController 관련
    PENALTY_ASSIGN_SUCCESS("페널티가 성공적으로 부여되었습니다."),
    PENALTY_FIND_ALL_SUCCESS("모든 패널티가 성공적으로 조회되었습니다."),
    PENALTY_DELETE_SUCCESS("패널티가 성공적으로 삭제되었습니다."),
    PENALTY_UPDATE_SUCCESS("패널티를 성공적으로 수정했습니다."),
    // penaltyUserController
    PENALTY_USER_FIND_SUCCESS("패널티가 성공적으로 조회되었습니다.");

    private final String message;
}
