package com.weeth.global.common.response

import org.springframework.data.domain.Page

data class PageResponse<T>(
    val content: List<T>,
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun <T> from(page: Page<T>): PageResponse<T> =
            PageResponse(
                content = page.content,
                pageNumber = page.number,
                pageSize = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages,
            )
    }
}
