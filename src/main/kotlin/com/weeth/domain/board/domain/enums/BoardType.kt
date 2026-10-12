package com.weeth.domain.board.domain.enums

/**
 * 게시판 타입.
 *
 * 주의: board.type은 네이티브 MySQL ENUM 컬럼이므로 값을 추가하면 Flyway 마이그레이션으로 ENUM 목록도 갱신해야 한다.
 *
 * @property countsTowardBoardLimit 동아리 게시판 개수 상한에 포함되는지 여부
 * @property includedInAllFeed 전체 게시판·대시보드 최근 글 같은 통합 피드에 글이 노출되는지 여부
 */
enum class BoardType(
    val countsTowardBoardLimit: Boolean = true,
    val includedInAllFeed: Boolean = true,
) {
    ALL, // 가상 전체 게시판 (DB에 저장되지 않음)
    NOTICE,
    GALLERY,
    GENERAL,
    INFORMATION,

    // 서비스 사용성/버그 제보 게시판. 운영진이 DB로 직접 제공하므로 동아리 상한을 차지하지 않고,
    // 제보 글이 일반 피드에 섞이지 않도록 통합 피드에서 제외한다.
    FEEDBACK(countsTowardBoardLimit = false, includedInAllFeed = false),
}
