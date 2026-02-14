package leets.weeth.domain.file.domain.entity;

import jakarta.persistence.*;
import leets.weeth.domain.account.domain.entity.Receipt;
import leets.weeth.domain.board.domain.entity.Notice;
import leets.weeth.domain.board.domain.entity.Post;
import leets.weeth.domain.comment.domain.entity.Comment;
import leets.weeth.global.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
public class File extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    private String fileUrl;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne
    @JoinColumn(name = "notice_id")
    private Notice notice;

    @ManyToOne
    @JoinColumn(name = "receipt_id")
    private Receipt receipt;

    @ManyToOne
    @JoinColumn(name = "comment_id")
    private Comment comment;

    public void update(String fileName, String fileUrl) {
        this.fileName = fileName;
        this.fileUrl = fileUrl;
    }
}
