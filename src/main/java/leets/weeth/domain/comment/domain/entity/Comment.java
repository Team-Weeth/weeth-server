package leets.weeth.domain.comment.domain.entity;


import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import leets.weeth.domain.board.domain.entity.Notice;
import leets.weeth.domain.board.domain.entity.Post;
import leets.weeth.domain.comment.application.dto.CommentDTO;
import leets.weeth.domain.user.domain.entity.User;
import leets.weeth.global.common.entity.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @Column(length = 300)
    private String content;

    @Column(nullable = false)
    private Boolean isDeleted;

    @ManyToOne
    @JoinColumn(name="post_id")
    @JsonBackReference
    private Post post;

    @ManyToOne
    @JoinColumn(name="notice_id")
    @JsonBackReference
    private Notice notice;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private List<Comment> children = new ArrayList<>();

    public void addChild(Comment child) {
        this.children.add(child);
    }

    @PrePersist
    public void prePersist() {
        if (isDeleted == null) {
            isDeleted = false;
        }
    }

    //TODO 문자열 상수처리
    public void markAsDeleted() {
        this.isDeleted = true;
        this.content = "삭제된 댓글입니다.";
    }

    public void update(CommentDTO.Update dto) {
            this.content = dto.content();
    }

}
