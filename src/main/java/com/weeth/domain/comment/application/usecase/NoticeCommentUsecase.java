package com.weeth.domain.comment.application.usecase;

import com.weeth.domain.comment.application.dto.CommentDTO;
import com.weeth.domain.user.application.exception.UserNotMatchException;

public interface NoticeCommentUsecase {

    void saveNoticeComment(CommentDTO.Save dto, Long noticeId, Long userId);

    void updateNoticeComment(CommentDTO.Update dto, Long noticeId, Long commentId, Long userId) throws UserNotMatchException;

    void deleteNoticeComment(Long commentId, Long userId) throws UserNotMatchException;
}
