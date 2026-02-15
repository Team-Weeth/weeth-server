package com.weeth.domain.board.domain.service;

import com.weeth.domain.board.application.dto.PostDTO;
import com.weeth.domain.board.domain.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostUpdateService {

    public void update(Post post, PostDTO.Update dto){
        post.update(dto);
    }

    public void updateEducation(Post post, PostDTO.UpdateEducation dto){
        post.updateEducation(dto);
    }
}
