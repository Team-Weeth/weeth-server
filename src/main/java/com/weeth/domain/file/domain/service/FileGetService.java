package com.weeth.domain.file.domain.service;

import com.weeth.domain.file.domain.entity.File;
import com.weeth.domain.file.domain.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileGetService {

    private final FileRepository fileRepository;

    public List<File> findAllByPost(Long postId) {
        return fileRepository.findAllByPostId(postId);
    }

    public List<File> findAllByNotice(Long noticeId) {
        return fileRepository.findAllByNoticeId(noticeId);
    }

    public List<File> findAllByReceipt(Long receiptId) {
        return fileRepository.findAllByReceiptId(receiptId);
    }

    public List<File> findAllByComment(Long commentId) {
        return fileRepository.findAllByCommentId(commentId);
    }
}
