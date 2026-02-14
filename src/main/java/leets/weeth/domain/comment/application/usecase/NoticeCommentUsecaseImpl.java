package leets.weeth.domain.comment.application.usecase;

import leets.weeth.domain.board.domain.entity.Notice;
import leets.weeth.domain.board.domain.service.NoticeFindService;
import leets.weeth.domain.comment.application.dto.CommentDTO;
import leets.weeth.domain.comment.application.exception.CommentNotFoundException;
import leets.weeth.domain.comment.application.mapper.CommentMapper;
import leets.weeth.domain.comment.domain.entity.Comment;
import leets.weeth.domain.comment.domain.service.CommentDeleteService;
import leets.weeth.domain.comment.domain.service.CommentFindService;
import leets.weeth.domain.comment.domain.service.CommentSaveService;
import leets.weeth.domain.file.application.mapper.FileMapper;
import leets.weeth.domain.file.domain.entity.File;
import leets.weeth.domain.file.domain.service.FileDeleteService;
import leets.weeth.domain.file.domain.service.FileGetService;
import leets.weeth.domain.file.domain.service.FileSaveService;
import leets.weeth.domain.user.application.exception.UserNotMatchException;
import leets.weeth.domain.user.domain.entity.User;
import leets.weeth.domain.user.domain.service.UserGetService;
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

    private final FileSaveService fileSaveService;
    private final FileGetService fileGetService;
    private final FileDeleteService fileDeleteService;
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

        List<File> files = fileMapper.toFileList(dto.files(), comment);
        fileSaveService.save(files);

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
        fileDeleteService.delete(fileList);

        List<File> files = fileMapper.toFileList(dto.files(), comment);
        fileSaveService.save(files);

        comment.update(dto);
    }

    @Override
    @Transactional
    public void deleteNoticeComment(Long commentId, Long userId) throws UserNotMatchException {
        User user = userGetService.find(userId);
        Comment comment = validateOwner(commentId, userId);
        Notice notice = comment.getNotice();

        List<File> fileList = getFiles(commentId);
        fileDeleteService.delete(fileList);

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
        return fileGetService.findAllByComment(commentId);
    }
}
