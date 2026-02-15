package com.weeth.domain.comment.test.fixture;

import com.weeth.domain.board.domain.entity.Notice;
import com.weeth.domain.comment.domain.entity.Comment;
import com.weeth.domain.user.domain.entity.User;

import java.util.ArrayList;

public class CommentTestFixture {
    public static Comment createComment(Long id, String content, User user, Notice noice){
        return Comment.builder()
                .id(id)
                .content(content)
                .notice(noice)
                .user(user)
                .children(new ArrayList<>())
                .isDeleted(Boolean.FALSE)
                .build();
    }
}
