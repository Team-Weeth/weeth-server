package com.weeth.domain.board.application.usecase;

import com.weeth.domain.board.application.dto.PartPostDTO;
import com.weeth.domain.board.application.dto.PostDTO;
import com.weeth.domain.board.application.exception.CategoryAccessDeniedException;
import com.weeth.domain.board.application.mapper.PostMapper;
import com.weeth.domain.board.domain.entity.Post;
import com.weeth.domain.board.domain.entity.enums.Category;
import com.weeth.domain.board.domain.entity.enums.Part;
import com.weeth.domain.board.domain.service.PostDeleteService;
import com.weeth.domain.board.domain.service.PostFindService;
import com.weeth.domain.board.domain.service.PostSaveService;
import com.weeth.domain.board.domain.service.PostUpdateService;
import com.weeth.domain.board.test.fixture.PostTestFixture;
import com.weeth.domain.comment.application.mapper.CommentMapper;
import com.weeth.domain.file.application.mapper.FileMapper;
import com.weeth.domain.file.domain.entity.File;
import com.weeth.domain.file.domain.service.FileDeleteService;
import com.weeth.domain.file.domain.service.FileGetService;
import com.weeth.domain.file.domain.service.FileSaveService;
import com.weeth.domain.file.test.fixture.FileTestFixture;
import com.weeth.domain.user.domain.entity.Cardinal;
import com.weeth.domain.user.domain.entity.User;
import com.weeth.domain.user.domain.service.CardinalGetService;
import com.weeth.domain.user.domain.service.UserCardinalGetService;
import com.weeth.domain.user.domain.service.UserGetService;
import com.weeth.domain.user.test.fixture.CardinalTestFixture;
import com.weeth.domain.user.test.fixture.UserTestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class PostUseCaseImplTest {

    @InjectMocks private PostUseCaseImpl postUseCase;

    @Mock private PostSaveService postSaveService;
    @Mock private PostFindService postFindService;
    @Mock private PostUpdateService postUpdateService;
    @Mock private PostDeleteService postDeleteService;

    @Mock private UserGetService userGetService;
    @Mock private UserCardinalGetService userCardinalGetService;
    @Mock private CardinalGetService cardinalGetService;

    @Mock private FileSaveService fileSaveService;
    @Mock private FileGetService fileGetService;
    @Mock private FileDeleteService fileDeleteService;

    @Mock private PostMapper mapper;
    @Mock private FileMapper fileMapper;
    @Mock private CommentMapper commentMapper;


    @Test
    @DisplayName("교육 게시글 저장 성공")
    void saveEducation() {
        Long userId = 1L;
        long postId = 1L;
        // given
        PostDTO.SaveEducation request = new PostDTO.SaveEducation("제목1", "내용",
                List.of(Part.BE), 1, List.of());

        User user  = UserTestFixture.createActiveUser1(1L);
        Post post = PostTestFixture.createPost(postId, "제목1", Category.Education);
        
        given(userGetService.find(userId)).willReturn(user);
        given(mapper.fromEducationDto(request, user)).willReturn(post);
        given(postSaveService.save(post)).willReturn(post);
        given(fileMapper.toFileList(request.files(), post)).willReturn(List.of());
        given(mapper.toSaveResponse(post)).willReturn(new PostDTO.SaveResponse(postId));

        // when
        PostDTO.SaveResponse response = postUseCase.saveEducation(request, userId);

        // then
        assertThat(response.id()).isEqualTo(postId);
        verify(userGetService).find(userId);
        verify(postSaveService).save(post);
        verify(mapper).toSaveResponse(post);

    }

    @Test
    @DisplayName("관리자 권한이 없는 사용자가 교육 게시글 생성 시 예외를 던진다")
    void saveEducation_unauthorizedUser_throwsException(){
        Long userId = 1L;
        PostDTO.Save request = new PostDTO.Save("제목", "내용", Category.Education,
                null, 1, Part.BE, 1, List.of());
        User user  = UserTestFixture.createActiveUser1(1L);

        given(userGetService.find(userId)).willReturn(user);

        // when & then
        assertThrows(CategoryAccessDeniedException.class, () -> postUseCase.save(request, userId));

    }

    @Test
    @DisplayName("특정 파트와 주차 조건으로 게시글 목록 조회 성공")
    void findPartPosts_success() {
        // given
        PartPostDTO dto = new PartPostDTO(
                Part.BE,
                Category.Education,
                1,
                2,
                "스터디1"
        );

        int pageNumber = 0;
        int pageSize = 5;
        User user = UserTestFixture.createActiveUser1();

        Post post1 = PostTestFixture.createEducationPost(1L, user, "게시글1", Category.Education, List.of(Part.BE),
                1, 1);
        Post post2 = PostTestFixture.createEducationPost(2L, user, "게시글2", Category.Education, List.of(Part.BE),
                1, 2);

        List<Post> postList = List.of(post2);
        Slice<Post> postSlice = new SliceImpl<>(postList);

        PostDTO.ResponseAll response2 = PostTestFixture.createResponseAll(post2);

        given(postFindService.findByPartAndOptionalFilters(
                dto.part(),
                dto.category(),
                dto.cardinalNumber(),
                dto.studyName(),
                dto.week(),
                PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "id"))
        )).willReturn(postSlice);

        given(mapper.toAll(post2, false)).willReturn(response2);

        // when
        Slice<PostDTO.ResponseAll> result = postUseCase.findPartPosts(dto, pageNumber, pageSize);

        // then
        // 2주차 게시글만 포함되어 있어야 함
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        PostDTO.ResponseAll first = result.getContent().get(0);

        assertThat(first.title()).isEqualTo("게시글2");
        assertThat(first.hasFile()).isFalse();

        verify(postFindService).findByPartAndOptionalFilters(
                dto.part(),
                dto.category(),
                dto.cardinalNumber(),
                dto.studyName(),
                dto.week(),
                PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "id"))
        );
    }

    @Test
    @DisplayName("관리자 권한 사용자가 교육 게시글 목록 조회 시 성공적으로 반환한다")
    void findEducationPosts_success_adminUser() {
        // given
        Long userId = 1L;
        Part part = Part.BE;
        Integer cardinalNumber = 1;
        int pageNumber = 0;
        int pageSize = 5;

        User adminUser = UserTestFixture.createAdmin(userId);

        Post post1 = PostTestFixture.createEducationPost(1L, adminUser, "교육글1", Category.Education, List.of(Part.BE), 1, 1);
        Post post2 = PostTestFixture.createEducationPost(2L, adminUser, "교육글2", Category.Education, List.of(Part.BE), 1, 2);
        List<Post> postList = List.of(post1, post2);
        Slice<Post> postSlice = new SliceImpl<>(postList);

        PostDTO.ResponseEducationAll response1 = PostTestFixture.createResponseEducationAll(post1, false);
        PostDTO.ResponseEducationAll response2 = PostTestFixture.createResponseEducationAll(post2, false);

        given(userGetService.find(userId)).willReturn(adminUser);
        given(postFindService.findByCategory(part, Category.Education, cardinalNumber, pageNumber, pageSize))
                .willReturn(postSlice);
        given(mapper.toEducationAll(post1, false)).willReturn(response1);
        given(mapper.toEducationAll(post2, false)).willReturn(response2);

        // when
        Slice<PostDTO.ResponseEducationAll> result =
                postUseCase.findEducationPosts(userId, part, cardinalNumber, pageNumber, pageSize);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(PostDTO.ResponseEducationAll::title)
                .containsExactly("교육글1", "교육글2");

        verify(postFindService).findByCategory(part, Category.Education, cardinalNumber, pageNumber, pageSize);
        verify(mapper).toEducationAll(post1, false);
        verify(mapper).toEducationAll(post2, false);
    }

    @Test
    @DisplayName("스터디가 없을 시 예외가 발생하지 않는다")
    void findStudyNames_noStudies_doesNotThrowException() {
        Part part = Part.BE;
        List<String> emptyNames = List.of();

        PostDTO.ResponseStudyNames expectedResponse = new PostDTO.ResponseStudyNames(emptyNames);

        given(postFindService.findByPart(part)).willReturn(emptyNames);
        given(mapper.toStudyNames(emptyNames)).willReturn(expectedResponse);

        // when & then
        assertThatCode(() -> postUseCase.findStudyNames(part))
                .doesNotThrowAnyException();

        verify(postFindService).findByPart(part);
        verify(mapper).toStudyNames(emptyNames);
    }

    @Test
    @DisplayName("본인이 속하지 않은 교육 자료를 검색하면 빈 리스트를 반환한다")
    void findEducationPosts_whenUserNotInCardinal_returnsEmptyList() {
        // given
        Long userId = 1L;
        Part part = Part.BE;
        Integer cardinalNumber = 3;
        int pageNumber = 0;
        int pageSize = 5;

        User user = UserTestFixture.createActiveUser1(userId);

        Cardinal cardinal = CardinalTestFixture.createCardinal(1, 2025, 1);

        given(userGetService.find(userId)).willReturn(user);
        given(cardinalGetService.findByUserSide(cardinalNumber)).willReturn(cardinal);
        given(userCardinalGetService.notContains(user, cardinal)).willReturn(true);

        // when
        Slice<PostDTO.ResponseEducationAll> result =
                postUseCase.findEducationPosts(userId, part, cardinalNumber, pageNumber, pageSize);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.hasNext()).isFalse();

        verify(userGetService).find(userId);
        verify(cardinalGetService).findByUserSide(cardinalNumber);
        verify(userCardinalGetService).notContains(user, cardinal);
        verify(postFindService, never()).findEducationByCardinal(any(), anyInt(), any(Pageable.class));
    }

    @Test
    @DisplayName("파일이 존재하는 경우 true를 반환한다")
    void fileExists_returnsTrue() {
        // given
        Long postId = 1L;
        File file = FileTestFixture.createFile(postId, "파일1", "url1");

        given(fileGetService.findAllByPost(postId)).willReturn(List.of(file));
        // when
        boolean fileExists = postUseCase.checkFileExistsByPost(postId);

        // then
        assertThat(fileExists).isTrue();
        verify(fileGetService).findAllByPost(postId);
    }
}
