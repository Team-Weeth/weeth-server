package com.weeth.domain.board.presentation

import com.weeth.domain.board.application.dto.request.CreatePostRequest
import com.weeth.domain.board.application.dto.request.UpdatePostRequest
import com.weeth.domain.board.application.dto.response.PostDetailResponse
import com.weeth.domain.board.application.dto.response.PostListResponse
import com.weeth.domain.board.application.dto.response.PostSaveResponse
import com.weeth.domain.board.application.exception.BoardErrorCode
import com.weeth.domain.board.application.usecase.command.ManagePostUseCase
import com.weeth.domain.board.application.usecase.query.GetPostQueryService
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.auth.jwt.exception.JwtErrorCode
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Slice
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "BOARD", description = "게시판 API")
@RestController
@RequestMapping("/api/v4/boards")
@ApiErrorCodeExample(BoardErrorCode::class)
class PostController(
    private val managePostUseCase: ManagePostUseCase,
    private val getPostQueryService: GetPostQueryService,
) {
    @PostMapping("/{boardId}/posts")
    @Operation(summary = "게시글 작성")
    fun save(
        @PathVariable boardId: Long,
        @RequestBody @Valid request: CreatePostRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<PostSaveResponse> =
        CommonResponse.success(BoardResponseCode.POST_CREATED_SUCCESS, managePostUseCase.save(boardId, request, userId))

    @GetMapping("/{boardId}/posts")
    @Operation(summary = "게시글 목록 조회")
    fun findPosts(
        @PathVariable boardId: Long,
        @RequestParam(defaultValue = "0") pageNumber: Int,
        @RequestParam(defaultValue = "10") pageSize: Int,
    ): CommonResponse<Slice<PostListResponse>> =
        CommonResponse.success(BoardResponseCode.POST_FIND_ALL_SUCCESS, getPostQueryService.findPosts(boardId, pageNumber, pageSize))

    @GetMapping("/posts/{postId}")
    @Operation(summary = "게시글 상세 조회")
    fun findPost(
        @PathVariable postId: Long,
    ): CommonResponse<PostDetailResponse> =
        CommonResponse.success(BoardResponseCode.POST_FIND_BY_ID_SUCCESS, getPostQueryService.findPost(postId))

    @PatchMapping("/posts/{postId}")
    @Operation(summary = "게시글 수정")
    fun update(
        @PathVariable postId: Long,
        @RequestBody @Valid request: UpdatePostRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<PostSaveResponse> =
        CommonResponse.success(BoardResponseCode.POST_UPDATED_SUCCESS, managePostUseCase.update(postId, request, userId))

    @DeleteMapping("/posts/{postId}")
    @Operation(summary = "게시글 삭제")
    fun delete(
        @PathVariable postId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        managePostUseCase.delete(postId, userId)
        return CommonResponse.success(BoardResponseCode.POST_DELETED_SUCCESS)
    }

    @GetMapping("/{boardId}/posts/search")
    @Operation(summary = "게시글 검색")
    fun searchPosts(
        @PathVariable boardId: Long,
        @RequestParam keyword: String,
        @RequestParam(defaultValue = "0") pageNumber: Int,
        @RequestParam(defaultValue = "10") pageSize: Int,
    ): CommonResponse<Slice<PostListResponse>> =
        CommonResponse.success(
            BoardResponseCode.POST_SEARCH_SUCCESS,
            getPostQueryService.searchPosts(boardId, keyword, pageNumber, pageSize),
        )
}
