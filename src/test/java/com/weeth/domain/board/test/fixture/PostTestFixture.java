package com.weeth.domain.board.test.fixture;

import com.weeth.domain.board.application.dto.PostDTO;
import com.weeth.domain.board.domain.entity.Post;
import com.weeth.domain.board.domain.entity.enums.Category;
import com.weeth.domain.board.domain.entity.enums.Part;
import com.weeth.domain.user.domain.entity.User;
import com.weeth.domain.user.domain.entity.enums.Role;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PostTestFixture {
    public static Post createPost(Long id, String title, Category category){
        return Post.builder()
                .id(id)
                .title(title)
                .content("내용")
                .comments(new ArrayList<>())
                .commentCount(0)
                .category(category)
                .build();
    }

    public static Post createEducationPost(Long id, User user, String title, Category category, List<Part> parts,
                                           int cardinalNumber, int week){
        return Post.builder()
                .id(id)
                .user(user)
                .title(title)
                .content("내용")
                .parts(parts)
                .cardinalNumber(cardinalNumber)
                .week(week)
                .commentCount(0)
                .category(Category.Education)
                .comments(new ArrayList<>())
                .build();
    }

    public static PostDTO.ResponseAll createResponseAll(Post post){
        return PostDTO.ResponseAll.builder()
                .id(post.getId())
                .part(post.getPart())
                .role(Role.USER)
                .title(post.getTitle())
                .content(post.getContent())
                .studyName(post.getStudyName())
                .week(post.getWeek())
                .time(LocalDateTime.now())
                .commentCount(post.getCommentCount())
                .hasFile(false)
                .isNew(false)
                .build();
    }

    public static PostDTO.ResponseEducationAll createResponseEducationAll(Post post, boolean fileExists) {
        return PostDTO.ResponseEducationAll.builder()
                .id(post.getId())
                .name(post.getUser().getName())
                .parts(post.getParts())
                .position(post.getUser().getPosition())
                .role(post.getUser().getRole())
                .title(post.getTitle())
                .content(post.getContent())
                .time(post.getCreatedAt())
                .commentCount(post.getCommentCount())
                .hasFile(fileExists)
                .isNew(false)
                .build();
    }


}
