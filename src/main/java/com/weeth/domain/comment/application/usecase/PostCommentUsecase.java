package com.weeth.domain.comment.application.usecase;

import com.weeth.domain.comment.application.dto.CommentDTO;
import com.weeth.domain.user.application.exception.UserNotMatchException;

public interface PostCommentUsecase {

    void savePostComment(CommentDTO.Save dto, Long postId, Long userId);

    void updatePostComment(CommentDTO.Update dto, Long postId, Long commentId, Long userId) throws UserNotMatchException;

    void deletePostComment(Long commentId, Long userId) throws UserNotMatchException;

}
