package com.weeth.domain.board.application.usecase;

import com.weeth.domain.board.application.dto.PartPostDTO;
import com.weeth.domain.board.application.dto.PostDTO;
import com.weeth.domain.board.domain.entity.enums.Part;
import com.weeth.domain.user.application.exception.UserNotMatchException;
import org.springframework.data.domain.Slice;


public interface PostUsecase {

    PostDTO.SaveResponse save(PostDTO.Save request, Long userId);

    PostDTO.SaveResponse saveEducation(PostDTO.SaveEducation request, Long userId);

    PostDTO.Response findPost(Long postId);

    Slice<PostDTO.ResponseAll> findPosts(int pageNumber, int pageSize);

    Slice<PostDTO.ResponseAll> findPartPosts(PartPostDTO dto, int pageNumber, int pageSize);

    Slice<PostDTO.ResponseEducationAll> findEducationPosts(Long userId, Part part, Integer cardinalNumber, int pageNumber, int pageSize);

    PostDTO.ResponseStudyNames findStudyNames(Part part);

    PostDTO.SaveResponse update(Long postId, PostDTO.Update dto, Long userId) throws UserNotMatchException;

    PostDTO.SaveResponse updateEducation(Long postId, PostDTO.UpdateEducation dto, Long userId) throws UserNotMatchException;

    void delete(Long postId, Long userId) throws UserNotMatchException;

    Slice<PostDTO.ResponseAll> searchPost(String keyword, int pageNumber, int pageSize);

    Slice<PostDTO.ResponseEducationAll> searchEducation(String keyword, int pageNumber, int pageSize);
}
