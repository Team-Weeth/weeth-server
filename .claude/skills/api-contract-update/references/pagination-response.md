# Pagination Response Reference

List responses wrap items plus shared `PageResponse`.

```kotlin
data class UserListResponse(
    val users: List<UserResponse>,
    val page: PageResponse,
)

data class PageResponse(
    val pageNumber: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
) {
    companion object {
        fun from(page: Page<*>) = PageResponse(
            pageNumber = page.number,
            pageSize = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            hasNext = page.hasNext(),
        )
    }
}
```

Keep list response names domain-specific (`UserListResponse`, `PostListResponse`) and reuse `PageResponse` instead of redefining page metadata.
