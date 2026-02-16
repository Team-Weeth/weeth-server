package com.weeth.domain.board.domain.service;

import com.weeth.domain.board.domain.entity.Post;
import com.weeth.domain.board.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostSaveService {

    private final PostRepository postRepository;

    public Post save(Post post) {
        return postRepository.save(post);
    }
}
