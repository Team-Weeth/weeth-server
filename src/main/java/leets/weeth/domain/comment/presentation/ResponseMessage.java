package leets.weeth.domain.comment.presentation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {
    // NoticeCommentController 관련
    COMMENT_CREATED_SUCCESS("공지사항 댓글이 성공적으로 생성되었습니다."),
    COMMENT_UPDATED_SUCCESS("공지사항 댓글이 성공적으로 수정되었습니다."),
    COMMENT_DELETED_SUCCESS("공지사항 댓글이 성공적으로 삭제되었습니다."),
    // PostCommentController 관련
    POST_COMMENT_CREATED_SUCCESS("게시글 댓글이 성공적으로 생성되었습니다."),
    POST_COMMENT_UPDATED_SUCCESS("게시글 댓글이 성공적으로 수정되었습니다."),
    POST_COMMENT_DELETED_SUCCESS("게시글 댓글이 성공적으로 삭제되었습니다.");

    private final String message;
}
