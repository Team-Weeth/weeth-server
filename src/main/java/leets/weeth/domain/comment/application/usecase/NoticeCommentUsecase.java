package leets.weeth.domain.comment.application.usecase;

import leets.weeth.domain.comment.application.dto.CommentDTO;
import leets.weeth.domain.user.application.exception.UserNotMatchException;

public interface NoticeCommentUsecase {

    void saveNoticeComment(CommentDTO.Save dto, Long noticeId, Long userId);

    void updateNoticeComment(CommentDTO.Update dto, Long noticeId, Long commentId, Long userId) throws UserNotMatchException;

    void deleteNoticeComment(Long commentId, Long userId) throws UserNotMatchException;
}
