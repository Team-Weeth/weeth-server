package com.weeth.domain.board.presentation

import com.weeth.domain.board.application.dto.request.CreatePostRequest
import com.weeth.domain.board.application.dto.request.UpdatePostRequest
import com.weeth.domain.board.application.dto.response.PostDetailResponse
import com.weeth.domain.board.application.dto.response.PostListResponse
import com.weeth.domain.board.application.dto.response.PostSaveResponse
import com.weeth.domain.board.application.exception.BoardErrorCode
import com.weeth.domain.board.application.usecase.command.ManagePostUseCase
import com.weeth.domain.board.application.usecase.command.MarkNoticeReadUseCase
import com.weeth.domain.board.application.usecase.query.GetPostQueryService
import com.weeth.domain.user.domain.enums.Role
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.auth.annotation.CurrentUserRole
import com.weeth.global.auth.jwt.application.exception.JwtErrorCode
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.web.TsidParam
import com.weeth.global.common.web.TsidPathVariable
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

@Tag(name = "BOARD", description = "게시글 API")
@RestController
@RequestMapping("/api/v4/clubs/{clubId}/boards")
@ApiErrorCodeExample(BoardErrorCode::class, JwtErrorCode::class)
class PostController(
    private val managePostUseCase: ManagePostUseCase,
    private val getPostQueryService: GetPostQueryService,
    private val markNoticeReadUseCase: MarkNoticeReadUseCase,
) {
    @PostMapping("/{boardId}/posts")
    @Operation(summary = "게시글 작성")
    fun save(
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable boardId: Long,
        @RequestBody @Valid request: CreatePostRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<PostSaveResponse> =
        CommonResponse.success(
            BoardResponseCode.POST_CREATED_SUCCESS,
            managePostUseCase.save(clubId, boardId, request, userId),
        )

    @GetMapping("/{boardId}/posts")
    @Operation(summary = "게시글 목록 조회")
    fun findPosts(
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable boardId: Long,
        @RequestParam(defaultValue = "0") pageNumber: Int,
        @RequestParam(defaultValue = "10") pageSize: Int,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @Parameter(hidden = true) @CurrentUserRole role: Role, // TODO: 멀티 테넨시 지원으로 Jwt에 포함한 Role은 삭제 예정
    ): CommonResponse<Slice<PostListResponse>> =
        CommonResponse.success(
            BoardResponseCode.POST_FIND_ALL_SUCCESS,
            getPostQueryService.findPosts(clubId, userId, boardId, pageNumber, pageSize, role),
        )

    @GetMapping("/posts/{postId}")
    @Operation(summary = "게시글 상세 조회")
    fun findPost(
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable postId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @Parameter(hidden = true) @CurrentUserRole role: Role, // TODO: 멀티 테넨시 지원으로 Jwt에 포함한 Role은 삭제 예정
    ): CommonResponse<PostDetailResponse> =
        CommonResponse.success(
            BoardResponseCode.POST_FIND_BY_ID_SUCCESS,
            getPostQueryService.findPost(clubId, userId, postId, role),
        )

    @PatchMapping("/posts/{postId}")
    @Operation(summary = "게시글 수정")
    fun update(
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable postId: Long,
        @RequestBody @Valid request: UpdatePostRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<PostSaveResponse> =
        CommonResponse.success(
            BoardResponseCode.POST_UPDATED_SUCCESS,
            managePostUseCase.update(clubId, postId, request, userId),
        )

    @DeleteMapping("/posts/{postId}")
    @Operation(summary = "게시글 삭제")
    fun delete(
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable postId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        managePostUseCase.delete(clubId, postId, userId)
        return CommonResponse.success(BoardResponseCode.POST_DELETED_SUCCESS)
    }

    @GetMapping("/{boardId}/posts/search")
    @Operation(summary = "게시글 검색")
    fun searchPosts(
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable boardId: Long,
        @RequestParam keyword: String,
        @RequestParam(defaultValue = "0") pageNumber: Int,
        @RequestParam(defaultValue = "10") pageSize: Int,
        @Parameter(hidden = true) @CurrentUser userId: Long,
        @Parameter(hidden = true) @CurrentUserRole role: Role, // TODO: 멀티 테넨시 지원으로 Jwt에 포함한 Role은 삭제 예정
    ): CommonResponse<Slice<PostListResponse>> =
        CommonResponse.success(
            BoardResponseCode.POST_SEARCH_SUCCESS,
            getPostQueryService.searchPosts(clubId, userId, boardId, keyword, pageNumber, pageSize, role),
        )

    @PostMapping("/{boardId}/notices/read-all")
    @Operation(summary = "공지 읽음 처리", description = "공지 게시판 진입 시 마지막 읽음 시간을 현재 시각으로 갱신합니다.")
    fun markAllNoticesRead(
        @PathVariable @TsidParam
        @TsidPathVariable clubId: Long,
        @PathVariable boardId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        markNoticeReadUseCase.execute(userId, clubId, boardId)
        return CommonResponse.success(BoardResponseCode.BOARD_NOTICE_READ_SUCCESS)
    }

    // todo: 좋아요 관련 API 추가
}
