package com.weeth.domain.board.domain.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import com.weeth.domain.board.application.dto.PostDTO;
import com.weeth.domain.board.domain.converter.PartListConverter;
import com.weeth.domain.board.domain.entity.enums.Category;
import com.weeth.domain.board.domain.entity.enums.Part;
import com.weeth.domain.comment.domain.entity.Comment;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Post extends Board {

    @Column
    private String studyName;

    @Column(nullable = false)
    private int cardinalNumber;

    @Column(nullable=false)
    private int week;

    @Enumerated(EnumType.STRING)
    private Part part;

    @Column(nullable = false, columnDefinition = "varchar(255)")
    @Convert(converter = PartListConverter.class)
    private List<Part> parts = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private Category category;

    @OneToMany(mappedBy = "post", orphanRemoval = true)
    @JsonManagedReference
    private List<Comment> comments;

    public void updateCommentCount() {
        this.updateCommentCount(this.comments);
    }

    public void addComment(Comment comment) {
        comments.add(comment);
    }

    public void update(PostDTO.Update dto) {
        this.updateUpperClass(dto);
        if (dto.studyName() != null)  this.studyName = dto.studyName();
        if (dto.week() != null)       this.week = dto.week();
        if (dto.part() != null) {
            this.part = dto.part();
            this.parts = List.of(dto.part());
        }
        if (dto.cardinalNumber() != null) this.cardinalNumber = dto.cardinalNumber();
    }

    public void updateEducation(PostDTO.UpdateEducation dto) {
        this.updateUpperClass(dto);
        this.part = null;
        if (dto.parts() != null) this.parts = dto.parts();
        if (dto.cardinalNumber() != null) this.cardinalNumber = dto.cardinalNumber();
    }
}
