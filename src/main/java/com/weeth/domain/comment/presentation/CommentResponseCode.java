package com.weeth.domain.comment.presentation;

import com.weeth.global.common.response.ResponseCodeInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CommentResponseCode implements ResponseCodeInterface {
    // NoticeCommentController 관련
    COMMENT_CREATED_SUCCESS(1400, HttpStatus.OK, "공지사항 댓글이 성공적으로 생성되었습니다."),
    COMMENT_UPDATED_SUCCESS(1401, HttpStatus.OK, "공지사항 댓글이 성공적으로 수정되었습니다."),
    COMMENT_DELETED_SUCCESS(1402, HttpStatus.OK, "공지사항 댓글이 성공적으로 삭제되었습니다."),
    // PostCommentController 관련
    POST_COMMENT_CREATED_SUCCESS(1403, HttpStatus.OK, "게시글 댓글이 성공적으로 생성되었습니다."),
    POST_COMMENT_UPDATED_SUCCESS(1404, HttpStatus.OK, "게시글 댓글이 성공적으로 수정되었습니다."),
    POST_COMMENT_DELETED_SUCCESS(1405, HttpStatus.OK, "게시글 댓글이 성공적으로 삭제되었습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;

    CommentResponseCode(int code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
