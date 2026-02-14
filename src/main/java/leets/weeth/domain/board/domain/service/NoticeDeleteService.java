package leets.weeth.domain.board.domain.service;

import jakarta.transaction.Transactional;
import leets.weeth.domain.board.domain.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NoticeDeleteService {

    private final NoticeRepository noticeRepository;

    @Transactional
    public void delete(Long noticeId) {
        noticeRepository.deleteById(noticeId);
    }

}
