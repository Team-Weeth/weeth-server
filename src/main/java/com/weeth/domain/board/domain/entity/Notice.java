package com.weeth.domain.board.domain.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import com.weeth.domain.board.application.dto.NoticeDTO;
import com.weeth.domain.comment.domain.entity.Comment;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Notice extends Board {

    // Todo: OneToMany 매핑 제거
    @OneToMany(mappedBy = "notice", orphanRemoval = true)
    @JsonManagedReference
    private List<Comment> comments;

    public void updateCommentCount() {
        this.updateCommentCount(this.comments);
    }

    public void addComment(Comment comment) {
        comments.add(comment);
    }

    public void update(NoticeDTO.Update dto){
        this.updateUpperClass(dto);
    }

}
