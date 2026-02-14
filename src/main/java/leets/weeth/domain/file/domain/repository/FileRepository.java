package leets.weeth.domain.file.domain.repository;

import leets.weeth.domain.file.domain.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileRepository extends JpaRepository<File, Long> {

    List<File> findAllByPostId(Long postId);

    List<File> findAllByNoticeId(Long noticeId);

    List<File> findAllByReceiptId(Long receiptId);

    List<File> findAllByCommentId(Long commentId);
}
