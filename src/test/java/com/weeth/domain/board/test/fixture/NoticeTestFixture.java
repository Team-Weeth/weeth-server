package com.weeth.domain.board.test.fixture;

import com.weeth.domain.board.domain.entity.Notice;
import com.weeth.domain.user.domain.entity.User;

import java.util.ArrayList;

public class NoticeTestFixture {
    public static Notice createNotice(String title, User user){
        return Notice.builder()
                .title(title)
                .content("내용")
                .user(user)
                .comments(new ArrayList<>())
                .commentCount(0)
                .build();
    }

    public static Notice createNotice(Long id, String title){
        return Notice.builder()
                .id(id)
                .title(title)
                .content("내용")
                .comments(new ArrayList<>())
                .commentCount(0)
                .build();
    }

    public static Notice createNotice(Long id, String title, User user){
        return Notice.builder()
                .id(id)
                .title(title)
                .content("내용")
                .user(user)
                .comments(new ArrayList<>())
                .commentCount(0)
                .build();
    }

    public static Notice createNotice(String title){
        return Notice.builder()
                .title(title)
                .content("내용")
                .comments(new ArrayList<>())
                .commentCount(0)
                .build();
    }
}
