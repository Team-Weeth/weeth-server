package com.weeth.domain.account.domain.enums

/**
 * 납부 대상 여부.
 *
 * 불변식: **"제외 여부"는 항상 `targetStatus == TARGETED`(의 부정)로만 판정한다.**
 * - "제외됨"은 두 형태로 존재한다: `EXCLUDED` 행이 있거나, **행이 아예 없거나(한 번도 선택되지 않은 명부 부원)**.
 *   따라서 제외 집계·목록은 `EXCLUDED` 행을 직접 세지 말고 `활성 명부 − 활성 TARGETED`로 파생해야 한다
 *   (행 없는 부원이 빠지지 않도록).
 * - `REFUNDED`(환불)는 제외가 아니다. 환불해도 `targetStatus`는 `TARGETED`로 유지되고 `paymentStatus`만 바뀐다.
 */
enum class AccountTargetStatus {
    TARGETED,
    EXCLUDED,
}
