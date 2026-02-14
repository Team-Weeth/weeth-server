package leets.weeth.domain.comment.domain.service;

import leets.weeth.domain.comment.domain.entity.Comment;
import leets.weeth.domain.comment.domain.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentSaveService {

    private final CommentRepository commentRepository;

    @Transactional
    public void save(Comment comment){
        commentRepository.save(comment);
    }

}
