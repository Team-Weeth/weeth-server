package leets.weeth.domain.board.domain.repository;

import leets.weeth.config.TestContainersConfig;
import leets.weeth.domain.board.domain.entity.Notice;
import leets.weeth.domain.board.test.fixture.NoticeTestFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DataJpaTest
@Import(TestContainersConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NoticeRepositoryTest {

    @Autowired
    private NoticeRepository noticeRepository;

    @Test
    void findPageBy_공지_id_내림차순으로_조회() {
        // given
        List<Notice> notices = new ArrayList<>();
        for(int i = 0; i<5; i++){
            Notice notice = NoticeTestFixture.createNotice("공지" + i);
            notices.add(notice);
        }

        noticeRepository.saveAll(notices);
        Pageable pageable = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "id"));

        // when
        Slice<Notice> pagedNotices = noticeRepository.findPageBy(pageable);

        // then
        assertThat(pagedNotices.getSize()).isEqualTo(3);
        assertThat(pagedNotices)
                .extracting(Notice::getTitle)
                        .containsExactly(
                                notices.get(4).getTitle(),
                                notices.get(3).getTitle(),
                                notices.get(2).getTitle()
                        );
        assertThat(pagedNotices.hasNext()).isTrue();
    }

    @Test
    void search_검색어가_포함된_공지_id_내림차순으로_조회() {
        // given
        List<Notice> notices = new ArrayList<>();
        for(int i = 0; i<6; i++){
            Notice notice;
            if(i % 2 == 0){
                notice = NoticeTestFixture.createNotice("공지" + i);
            } else{
                notice = NoticeTestFixture.createNotice("검색" + i);
            }
            notices.add(notice);
        }

        noticeRepository.saveAll(notices);

        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "id"));

        // when
        Slice<Notice> searchedNotices = noticeRepository.search("검색", pageable);

        // then
        assertThat(searchedNotices.getContent()).hasSize(3);
        assertThat(searchedNotices.getContent())
                .extracting(Notice::getTitle)
                        .containsExactly(notices.get(5).getTitle(),
                                notices.get(3).getTitle(),
                                notices.get(1).getTitle());
        assertThat(searchedNotices.hasNext()).isFalse();

    }
}
