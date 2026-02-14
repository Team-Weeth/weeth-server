package leets.weeth.domain.board.domain.service;

import leets.weeth.domain.board.domain.entity.Notice;
import leets.weeth.domain.board.domain.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NoticeSaveService {

    private final NoticeRepository noticeRepository;

    public Notice save(Notice notice){
        return noticeRepository.save(notice);
    }

}
