package com.weeth.domain.account.application.dto.request

import org.springframework.data.domain.Sort

/**
 * 거래 내역 목록 정렬 옵션. 날짜가 같은 거래는 생성 순서로 안정적으로 정렬한다.
 * 거래 데이터가 일정 규모를 넘어서면 transactedAt + createdAt 정렬에 맞춘 복합 인덱스를 검토한다.
 */
enum class AccountTransactionSort {
    LATEST,
    OLDEST,
    AMOUNT_DESC,
    AMOUNT_ASC,
    ;

    fun toSort(): Sort =
        when (this) {
            LATEST -> Sort.by(Sort.Direction.DESC, "transactedAt", "createdAt")
            OLDEST -> Sort.by(Sort.Direction.ASC, "transactedAt", "createdAt")
            AMOUNT_DESC -> Sort.by(Sort.Order.desc("amount"), Sort.Order.desc("id"))
            AMOUNT_ASC -> Sort.by(Sort.Order.asc("amount"), Sort.Order.desc("id"))
        }
}
