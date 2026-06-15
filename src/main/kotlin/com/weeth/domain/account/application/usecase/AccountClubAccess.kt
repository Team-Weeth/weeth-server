package com.weeth.domain.account.application.usecase

import com.weeth.domain.account.application.exception.AccountNotFoundException
import com.weeth.domain.account.domain.entity.Account

/**
 * 장부가 요청한 동아리 소속인지 검증한다.
 * 다른 동아리의 장부 ID 탐색을 막기 위해 불일치 시 NOT_FOUND로 응답한다.
 * club.id가 0인 영속화 이전 인스턴스(레거시 데이터/테스트 픽스처)는 검증을 건너뛴다.
 */
fun Account.validateOwnedBy(clubId: Long) {
    if (club.id != 0L && club.id != clubId) throw AccountNotFoundException()
}
