package com.weeth.domain.board.application.usecase;

import com.weeth.domain.board.application.dto.NoticeDTO;
import com.weeth.domain.board.application.mapper.NoticeMapper;
import com.weeth.domain.board.domain.entity.Notice;
import com.weeth.domain.board.domain.service.NoticeUpdateService;
import com.weeth.domain.board.test.fixture.NoticeTestFixture;
import com.weeth.domain.board.domain.service.NoticeFindService;
import com.weeth.domain.file.application.dto.request.FileSaveRequest;
import com.weeth.domain.file.application.mapper.FileMapper;
import com.weeth.domain.file.domain.entity.File;
import com.weeth.domain.file.domain.service.FileDeleteService;
import com.weeth.domain.file.domain.service.FileGetService;
import com.weeth.domain.file.domain.service.FileSaveService;
import com.weeth.domain.file.test.fixture.FileTestFixture;
import com.weeth.domain.user.domain.entity.User;
import com.weeth.domain.user.domain.entity.enums.Department;
import com.weeth.domain.user.domain.entity.enums.Position;
import com.weeth.domain.user.domain.entity.enums.Role;
import com.weeth.domain.user.test.fixture.UserTestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class NoticeUsecaseImplTest {

    @InjectMocks private NoticeUsecaseImpl noticeUsecase;

    @Mock private NoticeFindService noticeFindService;
    @Mock private NoticeUpdateService noticeUpdateService;

    @Mock private FileSaveService fileSaveService;
    @Mock private FileGetService fileGetService;
    @Mock private FileDeleteService fileDeleteService;

    @Mock private NoticeMapper noticeMapper;
    @Mock private FileMapper fileMapper;


    @Test
    void 공지사항이_최신순으로_정렬되는지() {
        // given
        User user = User.builder()
                .email("abc@test.com")
                .name("홍길동")
                .position(Position.BE)
                .department(Department.SW)
                .role(Role.USER)
                .build();

        List<Notice> notices = new ArrayList<>();
        for(int i = 0; i<5; i++){
            Notice notice = NoticeTestFixture.createNotice("공지" + i, user);
            ReflectionTestUtils.setField(notice, "id", (long) i + 1);
            notices.add(notice);
        }

        Pageable pageable = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "id"));

        Slice<Notice> slice = new SliceImpl<>(List.of(notices.get(4), notices.get(3), notices.get(2)), pageable, true);

        given(noticeFindService.findRecentNotices(any(Pageable.class))).willReturn(slice);
        given(fileGetService.findAllByNotice(any())).willReturn(List.of());

        given(noticeMapper.toAll(any(Notice.class), anyBoolean()))
                .willAnswer(invocation -> {
                    Notice notice = invocation.getArgument(0);
                    return new NoticeDTO.ResponseAll(
                            notice.getId(),
                            notice.getUser() != null ? notice.getUser().getName() : "",
                            notice.getUser() != null ? notice.getUser().getPosition() : Position.BE,
                            notice.getUser() != null ? notice.getUser().getRole() : Role.USER,
                            notice.getTitle(),
                            notice.getContent(),
                            notice.getCreatedAt(),
                            notice.getCommentCount(),
                            false
                    );
                });

        // when
        Slice<NoticeDTO.ResponseAll> noticeResponses = noticeUsecase.findNotices(0, 3);

        // then
        assertThat(noticeResponses).isNotNull();
        assertThat(noticeResponses.getContent()).hasSize(3);
        assertThat(noticeResponses.getContent())
                .extracting(NoticeDTO.ResponseAll::title)
                        .containsExactly(
                                notices.get(4).getTitle(),
                                notices.get(3).getTitle(),
                                notices.get(2).getTitle()
                        );
        assertThat(noticeResponses.hasNext()).isTrue();

        verify(noticeFindService, times(1)).findRecentNotices(pageable);
    }

    @Test
    void 공지사항_검색시_결과와_파일_존재여부가_정상적으로_반환() {
        // given
        User user = User.builder()
                .email("abc@test.com")
                .name("홍길동")
                .position(Position.BE)
                .department(Department.SW)
                .role(Role.USER)
                .build();

        List<Notice> notices = new ArrayList<>();
        for(int i = 0; i<3; i++){
            Notice notice = NoticeTestFixture.createNotice("공지" + i, user);
            ReflectionTestUtils.setField(notice, "id", (long) i + 1);
            notices.add(notice);
        }
        for(int i = 3; i<6; i++){
            Notice notice = NoticeTestFixture.createNotice("검색" + i, user);
            ReflectionTestUtils.setField(notice, "id", (long) i + 1);
            notices.add(notice);
        }


        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "id"));

        Slice<Notice> slice = new SliceImpl<>(List.of(notices.get(5), notices.get(4), notices.get(3)), pageable, false);

        given(noticeFindService.search(any(String.class), any(Pageable.class))).willReturn(slice);
        // 짝수 id - 파일 존재, 홀수 id - 파일 없음 (빈 리스트)
        given(fileGetService.findAllByNotice(any()))
                .willAnswer(invocation -> {
                    Long noticeId = invocation.getArgument(0);
                    if (noticeId % 2 == 0) {
                        return List.of(File.builder()
                                .notice(notices.get((int)(noticeId-1)))
                                .build());
                    } else {
                        return List.of();
                    }
                });

        given(noticeMapper.toAll(any(Notice.class), anyBoolean()))
                .willAnswer(invocation -> {
                    Notice notice = invocation.getArgument(0);
                    boolean fileExists = invocation.getArgument(1);
                    return new NoticeDTO.ResponseAll(
                            notice.getId(),
                            notice.getUser() != null ? notice.getUser().getName() : "",
                            notice.getUser() != null ? notice.getUser().getPosition() : Position.BE,
                            notice.getUser() != null ? notice.getUser().getRole() : Role.USER,
                            notice.getTitle(),
                            notice.getContent(),
                            notice.getCreatedAt(),
                            notice.getCommentCount(),
                            fileExists
                    );
                });

        // when
        Slice<NoticeDTO.ResponseAll> noticeResponses = noticeUsecase.searchNotice("검색", 0, 5);

        // then
        assertThat(noticeResponses).isNotNull();
        assertThat(noticeResponses.getContent()).hasSize(3);
        assertThat(noticeResponses.getContent())
                .extracting(NoticeDTO.ResponseAll::title)
                .containsExactly(
                        notices.get(5).getTitle(),
                        notices.get(4).getTitle(),
                        notices.get(3).getTitle()
                );
        assertThat(noticeResponses.hasNext()).isFalse();
        
        // 짝수 id : 파일 존재, 홀수 id : 파일 없음 검증
        assertThat(noticeResponses.getContent().get(0).hasFile()).isTrue();
        assertThat(noticeResponses.getContent().get(1).hasFile()).isFalse();

        verify(noticeFindService, times(1)).search("검색", pageable);
    }

    @Test
    @DisplayName("공지사항 수정 시 기존 파일 삭제 후 새 파일로 업데이트된다")
    void update_replacesOldFilesWithNewFiles() {
        // given
        Long noticeId = 1L;
        Long userId = 1L;

        User user = UserTestFixture.createActiveUser1(userId);
        Notice notice = NoticeTestFixture.createNotice(noticeId, "기존 제목", user);

        File oldFile = FileTestFixture.createFile(1L, "old.pdf", "https://example.com/old.pdf", notice);
        List<File> oldFiles = List.of(oldFile);

        NoticeDTO.Update dto = new NoticeDTO.Update("수정된 제목", "수정된 내용",
                List.of(new FileSaveRequest("new.pdf", "https://example.com/new.pdf")));

        File newFile = FileTestFixture.createFile(2L, "new.pdf", "https://example.com/new.pdf", notice);
        List<File> newFiles = List.of(newFile);

        NoticeDTO.SaveResponse expectedResponse = new NoticeDTO.SaveResponse(noticeId);

        given(noticeFindService.find(noticeId)).willReturn(notice);
        given(fileGetService.findAllByNotice(noticeId)).willReturn(oldFiles);
        given(fileMapper.toFileList(dto.files(), notice)).willReturn(newFiles);
        given(noticeMapper.toSaveResponse(notice)).willReturn(expectedResponse);

        // when
        NoticeDTO.SaveResponse response = noticeUsecase.update(noticeId, dto, userId);

        // then
        assertThat(response).isEqualTo(expectedResponse);

        verify(noticeFindService).find(noticeId);
        verify(fileGetService).findAllByNotice(noticeId);
        verify(fileDeleteService).delete(oldFiles);
        verify(fileMapper).toFileList(dto.files(), notice);
        verify(fileSaveService).save(newFiles);
        verify(noticeUpdateService).update(notice, dto);
    }

    @Test
    @DisplayName("공지사항 엔티티 update() 호출 시 제목과 내용이 변경된다")
    void update_updatesTitleAndContent() {
        // given
        Long userId = 1L;
        User user = UserTestFixture.createActiveUser1(userId);
        Notice notice = NoticeTestFixture.createNotice(1L, "기존 제목", user);
        NoticeDTO.Update dto = new NoticeDTO.Update("수정된 제목", "수정된 내용", List.of());

        // when
        notice.update(dto);

        // then
        assertThat(notice.getTitle()).isEqualTo(dto.title());
        assertThat(notice.getContent()).isEqualTo(dto.content());
    }
}
