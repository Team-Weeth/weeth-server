package com.weeth.domain.board.application.usecase;

import com.weeth.domain.board.application.dto.PartPostDTO;
import com.weeth.domain.board.application.dto.PostDTO;
import com.weeth.domain.board.application.exception.CategoryAccessDeniedException;
import com.weeth.domain.board.application.exception.NoSearchResultException;
import com.weeth.domain.board.application.exception.PageNotFoundException;
import com.weeth.domain.board.application.mapper.PostMapper;
import com.weeth.domain.board.domain.entity.Post;
import com.weeth.domain.board.domain.entity.enums.Category;
import com.weeth.domain.board.domain.entity.enums.Part;
import com.weeth.domain.board.domain.service.PostDeleteService;
import com.weeth.domain.board.domain.service.PostFindService;
import com.weeth.domain.board.domain.service.PostSaveService;
import com.weeth.domain.board.domain.service.PostUpdateService;
import com.weeth.domain.comment.application.dto.response.CommentResponse;
import com.weeth.domain.comment.application.usecase.query.GetCommentQueryService;
import com.weeth.domain.comment.domain.entity.Comment;
import com.weeth.domain.file.application.dto.response.FileResponse;
import com.weeth.domain.file.application.mapper.FileMapper;
import com.weeth.domain.file.domain.entity.File;
import com.weeth.domain.file.domain.entity.FileOwnerType;
import com.weeth.domain.file.domain.repository.FileReader;
import com.weeth.domain.file.domain.repository.FileRepository;
import com.weeth.domain.user.application.exception.UserNotMatchException;
import com.weeth.domain.user.domain.entity.User;
import com.weeth.domain.user.domain.entity.enums.Role;
import com.weeth.domain.user.domain.service.CardinalGetService;
import com.weeth.domain.user.domain.service.UserCardinalGetService;
import com.weeth.domain.user.domain.service.UserGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostUseCaseImpl implements PostUsecase {

    private final PostSaveService postSaveService;
    private final PostFindService postFindService;
    private final PostUpdateService postUpdateService;
    private final PostDeleteService postDeleteService;

    private final UserGetService userGetService;
    private final UserCardinalGetService userCardinalGetService;
    private final CardinalGetService cardinalGetService;

    private final FileRepository fileRepository;
    private final FileReader fileReader;

    private final PostMapper mapper;
    private final FileMapper fileMapper;
    private final GetCommentQueryService getCommentQueryService;

    @Override
    @Transactional
    public PostDTO.SaveResponse save(PostDTO.Save request, Long userId) {
        User user = userGetService.find(userId);

        if (request.category() == Category.Education
                && !user.hasRole(Role.ADMIN)) {
            throw new CategoryAccessDeniedException();
        }

        cardinalGetService.findByUserSide(request.cardinalNumber());
        Post post = mapper.fromPostDto(request, user);
        Post savedPost = postSaveService.save(post);

        List<File> files = fileMapper.toFileList(request.files(), FileOwnerType.POST, savedPost.getId());
        fileRepository.saveAll(files);

        return mapper.toSaveResponse(savedPost);
    }

    @Override
    @Transactional
    public PostDTO.SaveResponse saveEducation(PostDTO.SaveEducation request, Long userId) {
        User user = userGetService.find(userId);

        Post post = mapper.fromEducationDto(request, user);
        Post saverPost = postSaveService.save(post);

        List<File> files = fileMapper.toFileList(request.files(), FileOwnerType.POST, saverPost.getId());
        fileRepository.saveAll(files);

        return mapper.toSaveResponse(saverPost);
    }

    @Override
    public PostDTO.Response findPost(Long postId) {
        Post post = postFindService.find(postId);

        List<FileResponse> response = getFiles(postId).stream()
                .map(fileMapper::toFileResponse)
                .toList();

        return mapper.toPostDto(post, response, filterParentComments(post.getComments()));
    }

    @Override
    public Slice<PostDTO.ResponseAll> findPosts(int pageNumber, int pageSize) {
        validatePageNumber(pageNumber);

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Slice<Post> posts = postFindService.findRecentPosts(pageable);

        return posts.map(post->mapper.toAll(post, checkFileExistsByPost(post.id)));
    }

    @Override
    public Slice<PostDTO.ResponseAll> findPartPosts(PartPostDTO dto, int pageNumber, int pageSize) {
        validatePageNumber(pageNumber);

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Slice<Post> posts = postFindService.findByPartAndOptionalFilters(dto.part(), dto.category(), dto.cardinalNumber(), dto.studyName(), dto.week(), pageable);

        return posts.map(post->mapper.toAll(post, checkFileExistsByPost(post.id)));
    }

    @Override
    public Slice<PostDTO.ResponseEducationAll> findEducationPosts(Long userId, Part part, Integer cardinalNumber, int pageNumber, int pageSize) {
        User user = userGetService.find(userId);
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "id"));

        if (user.hasRole(Role.ADMIN)) {

            return postFindService.findByCategory(part, Category.Education, cardinalNumber, pageNumber, pageSize)
                    .map(post -> mapper.toEducationAll(post, checkFileExistsByPost(post.getId())));
        }

        if (cardinalNumber != null) {
            if (userCardinalGetService.notContains(user, cardinalGetService.findByUserSide(cardinalNumber))) {
                return new SliceImpl<>(Collections.emptyList(), pageable, false);
            }
            Slice<Post> posts = postFindService.findEducationByCardinal(part, cardinalNumber, pageable);
            return posts.map(post -> mapper.toEducationAll(post, checkFileExistsByPost(post.getId())));
        }

        List<Integer> userCardinals = userCardinalGetService.getCardinalNumbers(user);
        if (userCardinals.isEmpty()) {
            return new SliceImpl<>(Collections.emptyList(), pageable, false);
        }
        Slice<Post> posts = postFindService.findEducationByCardinals(part, userCardinals, pageable);

        return posts.map(post -> mapper.toEducationAll(post, checkFileExistsByPost(post.getId())));
    }

    @Override
    public PostDTO.ResponseStudyNames findStudyNames(Part part) {
        List<String> names = postFindService.findByPart(part);

        return mapper.toStudyNames(names);
    }

    @Override
    public Slice<PostDTO.ResponseAll> searchPost(String keyword, int pageNumber, int pageSize){
        validatePageNumber(pageNumber);

        keyword = keyword.strip();  // 문자열 앞뒤 공백 제거

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Slice<Post> posts = postFindService.search(keyword, pageable);

        if(posts.isEmpty()){
            throw new NoSearchResultException();
        }

        return posts.map(post->mapper.toAll(post, checkFileExistsByPost(post.id)));
    }

    @Override
    public Slice<PostDTO.ResponseEducationAll> searchEducation(String keyword, int pageNumber, int pageSize) {
        validatePageNumber(pageNumber);

        keyword = keyword.strip();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Slice<Post> posts = postFindService.searchEducation(keyword, pageable);

        if(posts.isEmpty()){
            throw new NoSearchResultException();
        }

        return posts.map(post->mapper.toEducationAll(post, checkFileExistsByPost(post.id)));
    }

    @Override
    @Transactional
    public PostDTO.SaveResponse update(Long postId, PostDTO.Update dto, Long userId) {
        Post post = validateOwner(postId, userId);

        if (dto.files() != null) {
            List<File> fileList = getFiles(postId);
            fileRepository.deleteAll(fileList);

            List<File> files = fileMapper.toFileList(dto.files(), FileOwnerType.POST, post.getId());
            fileRepository.saveAll(files);
        }

        postUpdateService.update(post, dto);

        return mapper.toSaveResponse(post);
    }

    @Override
    @Transactional
    public PostDTO.SaveResponse updateEducation(Long postId, PostDTO.UpdateEducation dto, Long userId) {
        Post post = validateOwner(postId, userId);

        if (dto.files() != null) {
            List<File> fileList = getFiles(postId);
            fileRepository.deleteAll(fileList);

            List<File> files = fileMapper.toFileList(dto.files(), FileOwnerType.POST, post.getId());
            fileRepository.saveAll(files);
        }

        postUpdateService.updateEducation(post, dto);

        return mapper.toSaveResponse(post);
    }

    @Override
    @Transactional
    public void delete(Long postId, Long userId) {
        validateOwner(postId, userId);

        List<File> fileList = getFiles(postId);
        fileRepository.deleteAll(fileList);

        postDeleteService.delete(postId);
    }

    private List<File> getFiles(Long postId) {
        return fileReader.findAll(FileOwnerType.POST, postId, null);
    }

    private Post validateOwner(Long postId, Long userId) {
        Post post = postFindService.find(postId);

        if (!post.getUser().getId().equals(userId)) {
            throw new UserNotMatchException();
        }
        return post;
    }

    public boolean checkFileExistsByPost(Long postId){
        return fileReader.exists(FileOwnerType.POST, postId, null);
    }

    private List<CommentResponse> filterParentComments(List<Comment> comments) {
        return getCommentQueryService.toCommentTreeResponses(comments);
    }

    private void validatePageNumber(int pageNumber){
        if (pageNumber < 0) {
            throw new PageNotFoundException();
        }
    }

}
