package com.weeth.domain.comment.domain.service;

import com.weeth.domain.comment.domain.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentDeleteService {

    private final CommentRepository commentRepository;

    @Transactional
    public void delete(Long commentId) {
        commentRepository.deleteById(commentId);
    }

}
