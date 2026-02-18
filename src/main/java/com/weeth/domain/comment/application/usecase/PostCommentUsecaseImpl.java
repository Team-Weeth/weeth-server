package com.weeth.domain.comment.application.usecase;

import com.weeth.domain.board.domain.entity.Post;
import com.weeth.domain.board.domain.service.PostFindService;
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
public class PostCommentUsecaseImpl implements PostCommentUsecase {

    private final CommentSaveService commentSaveService;
    private final CommentFindService commentFindService;
    private final CommentDeleteService commentDeleteService;

    private final FileRepository fileRepository;
    private final FileReader fileReader;
    private final FileMapper fileMapper;

    private final UserGetService userGetService;

    private final PostFindService postFindService;

    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public void savePostComment(CommentDTO.Save dto, Long postId, Long userId) {
        User user = userGetService.find(userId);
        Post post = postFindService.find(postId);
        Comment parentComment = null;

        if (!(dto.parentCommentId() == null)) {
            parentComment = commentFindService.find(dto.parentCommentId());
        }
        Comment comment = commentMapper.fromCommentDto(dto, post, user, parentComment);
        commentSaveService.save(comment);

        List<File> files = fileMapper.toFileList(dto.files(), FileOwnerType.COMMENT, comment.getId());
        fileRepository.saveAll(files);

        // 부모 댓글이 없다면 새 댓글로 추가
        if (parentComment == null) {
            post.addComment(comment);
        } else {
            // 부모 댓글이 있다면 자녀 댓글로 추가
            parentComment.addChild(comment);
        }
        post.updateCommentCount();
    }

    @Override
    @Transactional
    public void updatePostComment(CommentDTO.Update dto, Long postId, Long commentId, Long userId) throws UserNotMatchException {
        User user = userGetService.find(userId);
        Post post = postFindService.find(postId);
        Comment comment = validateOwner(commentId, userId);

        List<File> fileList = getFiles(commentId);
        fileRepository.deleteAll(fileList);

        List<File> files = fileMapper.toFileList(dto.files(), FileOwnerType.COMMENT, comment.getId());
        fileRepository.saveAll(files);

        comment.update(dto);
    }


    @Override
    @Transactional
    public void deletePostComment(Long commentId, Long userId) throws UserNotMatchException {
        User user = userGetService.find(userId);
        Comment comment = validateOwner(commentId, userId);
        Post post = comment.getPost();

        List<File> fileList = getFiles(commentId);
        fileRepository.deleteAll(fileList);

        /*
        1. 지우고자 하는 댓글이 맨 아래층인 경우(child, child가 없는 댓글
            - 현재 댓글.getChildren이 NULL 이면 해당
            - 내가 child인지 child가 없는 댓글인지 구분해야함
            - child인 경우 -> 부모가 있음. 하지만 부모를 삭제하는게 아니라 나만 삭제함, 부모의 childern에서 나를 제거해야함
            - child가 없는 댓글인 경우 -> 자식이 없기 떄문에 나만 삭제함
         */
        // 현재 삭제하고자 하는 댓글이 자식이 없는 경우
        if (comment.getChildren().isEmpty()) {
            Comment parentComment = findParentComment(commentId);
            commentDeleteService.delete(commentId);
            if (parentComment != null) {
                parentComment.getChildren().remove(comment);
                if (parentComment.getIsDeleted() && parentComment.getChildren().isEmpty()) {
                    post.getComments().remove(parentComment);
                    commentDeleteService.delete(parentComment.getId());
                }
            }
        } else if (comment.getIsDeleted()) { // 삭제된 대댓글인 경우 예외
            throw new CommentNotFoundException();
        } else {
            comment.markAsDeleted();
            commentSaveService.save(comment);
        }
        post.decreaseCommentCount();
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

    // 업데이트 메소드를 엔티티 안에서 변경감지로 사용하기로 했기 때문에, 반환 값이 필요 없짐 -> 나머지도 다 수정
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
