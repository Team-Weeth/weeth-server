package leets.weeth.domain.board.application.mapper;

import leets.weeth.domain.board.application.dto.PostDTO;
import leets.weeth.domain.board.domain.entity.Post;
import leets.weeth.domain.user.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PostMapperTest {

    @InjectMocks
    private PostMapper mapper = Mappers.getMapper(PostMapper.class);

    private User testUser;
    private Post testPost;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("테스트유저")
                .email("test@weeth.com")
                .build();

        testPost = Post.builder()
                .id(1L)
                .title("테스트 게시글")
                .user(testUser)
                .content("테스트 내용입니다.")
                .build();
    }

    @Test
    @DisplayName("Post를 PostDTO.SaveResponse로 변환")
    void toSaveResponse() {
        // given
        // testPost 사용

        // when
        PostDTO.SaveResponse response = mapper.toSaveResponse(testPost);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(testPost.getId());
    }

}
