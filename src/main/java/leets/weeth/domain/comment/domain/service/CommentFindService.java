package leets.weeth.domain.comment.domain.service;

import leets.weeth.domain.comment.domain.entity.Comment;
import leets.weeth.domain.comment.domain.repository.CommentRepository;
import leets.weeth.domain.comment.application.exception.CommentNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentFindService {

    private final CommentRepository commentRepository;

    public Comment find(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);
    }

    public List<Comment> find() {
        return commentRepository.findAll();
    }

}
