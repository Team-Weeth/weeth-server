package com.weeth.domain.comment.presentation

import com.weeth.domain.comment.application.dto.request.CommentSaveRequest
import com.weeth.domain.comment.application.dto.request.CommentUpdateRequest
import com.weeth.domain.comment.application.exception.CommentErrorCode
import com.weeth.domain.comment.application.usecase.command.NoticeCommentUsecase
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "COMMENT-NOTICE", description = "공지사항 댓글 API")
@RestController
@RequestMapping("/api/v1/notices/{noticeId}/comments")
@ApiErrorCodeExample(CommentErrorCode::class)
class NoticeCommentController(
    private val noticeCommentUsecase: NoticeCommentUsecase,
) {
    @PostMapping
    @Operation(summary = "공지사항 댓글 작성")
    fun saveNoticeComment(
        @RequestBody @Valid dto: CommentSaveRequest,
        @PathVariable noticeId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<String> {
        noticeCommentUsecase.saveNoticeComment(dto, noticeId, userId)
        return CommonResponse.success(CommentResponseCode.COMMENT_CREATED_SUCCESS)
    }

    @PatchMapping("/{commentId}")
    @Operation(
        summary = "공지사항 댓글 수정",
        description = "files 규약: null=기존 첨부 유지, []=기존 첨부 전체 삭제, 배열 전달=전달 목록으로 교체",
    )
    fun updateNoticeComment(
        @RequestBody @Valid dto: CommentUpdateRequest,
        @PathVariable noticeId: Long,
        @PathVariable commentId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<String> {
        noticeCommentUsecase.updateNoticeComment(dto, noticeId, commentId, userId)
        return CommonResponse.success(CommentResponseCode.COMMENT_UPDATED_SUCCESS)
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "공지사항 댓글 삭제")
    fun deleteNoticeComment(
        @PathVariable noticeId: Long,
        @PathVariable commentId: Long,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<String> {
        noticeCommentUsecase.deleteNoticeComment(noticeId, commentId, userId)
        return CommonResponse.success(CommentResponseCode.COMMENT_DELETED_SUCCESS)
    }
}
