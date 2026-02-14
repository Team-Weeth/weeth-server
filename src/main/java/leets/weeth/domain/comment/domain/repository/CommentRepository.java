package leets.weeth.domain.comment.domain.repository;

import leets.weeth.domain.comment.domain.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
