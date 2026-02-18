package com.weeth.domain.comment.application.usecase;

import com.weeth.domain.board.domain.entity.Notice;
import com.weeth.domain.board.domain.service.NoticeFindService;
import com.weeth.domain.comment.application.dto.CommentDTO;
import com.weeth.domain.comment.application.exception.CommentNotFoundException;
import com.weeth.domain.comment.application.mapper.CommentMapper;
import com.weeth.domain.comment.domain.entity.Comment;
import com.weeth.domain.comment.domain.service.CommentDeleteService;
import com.weeth.domain.comment.domain.service.CommentFindService;
import com.weeth.domain.comment.domain.service.CommentSaveService;
import com.weeth.domain.file.application.mapper.FileMapper;
import com.weeth.domain.file.domain.entity.File;
import com.weeth.domain.file.domain.entity.FileOwnerType;
import com.weeth.domain.file.domain.repository.FileReader;
import com.weeth.domain.file.domain.repository.FileRepository;
import com.weeth.domain.user.application.exception.UserNotMatchException;
import com.weeth.domain.user.domain.entity.User;
import com.weeth.domain.user.domain.service.UserGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeCommentUsecaseImpl implements NoticeCommentUsecase {

    private final CommentSaveService commentSaveService;
    private final CommentFindService commentFindService;
    private final CommentDeleteService commentDeleteService;

    private final FileRepository fileRepository;
    private final FileReader fileReader;
    private final FileMapper fileMapper;

    private final NoticeFindService noticeFindService;

    private final UserGetService userGetService;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public void saveNoticeComment(CommentDTO.Save dto, Long noticeId, Long userId) {
        User user = userGetService.find(userId);
        Notice notice = noticeFindService.find(noticeId);
        Comment parentComment = null;

        if(!(dto.parentCommentId() == null)) {
            parentComment = commentFindService.find(dto.parentCommentId());
        }
        Comment comment = commentMapper.fromCommentDto(dto, notice, user, parentComment);
        commentSaveService.save(comment);

        List<File> files = fileMapper.toFileList(dto.files(), FileOwnerType.COMMENT, comment.getId());
        fileRepository.saveAll(files);

        // 부모 댓글이 없다면 새 댓글로 추가
        if(parentComment == null) {
            notice.addComment(comment);
        } else {
            // 부모 댓글이 있다면 자녀 댓글로 추가
            parentComment.addChild(comment);
        }
        notice.updateCommentCount();
    }

    @Override
    @Transactional
    public void updateNoticeComment(CommentDTO.Update dto, Long noticeId, Long commentId, Long userId) throws UserNotMatchException {
        User user = userGetService.find(userId);
        Notice notice = noticeFindService.find(noticeId);
        Comment comment = validateOwner(commentId, userId);

        List<File> fileList = getFiles(commentId);
        fileRepository.deleteAll(fileList);

        List<File> files = fileMapper.toFileList(dto.files(), FileOwnerType.COMMENT, comment.getId());
        fileRepository.saveAll(files);

        comment.update(dto);
    }

    @Override
    @Transactional
    public void deleteNoticeComment(Long commentId, Long userId) throws UserNotMatchException {
        User user = userGetService.find(userId);
        Comment comment = validateOwner(commentId, userId);
        Notice notice = comment.getNotice();

        List<File> fileList = getFiles(commentId);
        fileRepository.deleteAll(fileList);

        if (comment.getChildren().isEmpty()) {
            Comment parentComment = findParentComment(commentId);
            commentDeleteService.delete(commentId);
            if (parentComment != null) {
                parentComment.getChildren().remove(comment);
                if (parentComment.getIsDeleted() && parentComment.getChildren().isEmpty()) {
                    notice.getComments().remove(parentComment);
                    commentDeleteService.delete(parentComment.getId());
                }
            }
        } else if (comment.getIsDeleted()) { // 삭제된 대댓글인 경우 예외
            throw new CommentNotFoundException();
        } else {
            comment.markAsDeleted();
            commentSaveService.save(comment);
        }
        notice.decreaseCommentCount();
    }

    private Comment findParentComment(Long commentId) {
        List<Comment> comments = commentFindService.find();
        for (Comment comment : comments) {
            if (comment.getChildren().stream().anyMatch(child -> child.getId().equals(commentId))) {
                return comment;
            }
        }
        return null; // 부모 댓글을 찾지 못한 경우
    }

    private Comment validateOwner(Long commentId, Long userId) throws UserNotMatchException {
        Comment comment = commentFindService.find(commentId);

        if (!comment.getUser().getId().equals(userId)) {
            throw new UserNotMatchException();
        }
        return comment;
    }

    private List<File> getFiles(Long commentId) {
        return fileReader.findAll(FileOwnerType.COMMENT, commentId, null);
    }
}
