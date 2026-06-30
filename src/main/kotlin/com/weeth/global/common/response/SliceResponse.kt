package com.weeth.global.common.response

import org.springframework.data.domain.Slice

/**
 * 무한 스크롤 목록 응답 래퍼.
 * 전체 개수(totalElements/totalPages)를 계산하지 않는 대신 다음 페이지 존재 여부(`hasNext`)만 제공한다.
 * 전체 개수가 필요한 화면은 [PageResponse] 를 사용한다.
 */
data class SliceResponse<T>(
    val content: List<T>,
    val pageNumber: Int,
    val pageSize: Int,
    val numberOfElements: Int,
    val hasNext: Boolean,
) {
    companion object {
        fun <T> from(slice: Slice<T>): SliceResponse<T> =
            SliceResponse(
                content = slice.content,
                pageNumber = slice.number,
                pageSize = slice.size,
                numberOfElements = slice.numberOfElements,
                hasNext = slice.hasNext(),
            )
    }
}
