package com.weeth.domain.account.application.dto.request

import org.springframework.data.domain.Sort

/**
 * 거래 내역 목록 정렬 옵션. id 를 보조 정렬키로 두어 동일 값에서도 안정적으로 정렬한다.
 */
enum class AccountTransactionSort {
    LATEST,
    OLDEST,
    AMOUNT_DESC,
    AMOUNT_ASC,
    ;

    fun toSort(): Sort =
        when (this) {
            LATEST -> Sort.by(Sort.Direction.DESC, "transactedAt", "id")
            OLDEST -> Sort.by(Sort.Direction.ASC, "transactedAt", "id")
            AMOUNT_DESC -> Sort.by(Sort.Order.desc("amount"), Sort.Order.desc("id"))
            AMOUNT_ASC -> Sort.by(Sort.Order.asc("amount"), Sort.Order.desc("id"))
        }
}
