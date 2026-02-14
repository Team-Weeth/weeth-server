package leets.weeth.domain.board.domain.service;

import java.util.List;
import leets.weeth.domain.board.application.exception.NoticeNotFoundException;
import leets.weeth.domain.board.domain.entity.Notice;
import leets.weeth.domain.board.domain.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NoticeFindService {

    private final NoticeRepository noticeRepository;

    public Notice find(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(NoticeNotFoundException::new);
    }

    public List<Notice> find() {
        return noticeRepository.findAll();
    }


    public Slice<Notice> findRecentNotices(Pageable pageable) {
        return noticeRepository.findPageBy(pageable);
    }

    public Slice<Notice> search(String keyword, Pageable pageable) {
        if(keyword == null || keyword.isEmpty()){
            return findRecentNotices(pageable);
        }
        return noticeRepository.search(keyword.strip(), pageable);
    }
}
