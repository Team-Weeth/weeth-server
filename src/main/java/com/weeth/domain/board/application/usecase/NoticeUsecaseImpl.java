package com.weeth.domain.board.application.usecase;

import com.weeth.domain.board.application.dto.NoticeDTO;
import com.weeth.domain.board.application.exception.NoSearchResultException;
import com.weeth.domain.board.application.exception.PageNotFoundException;
import com.weeth.domain.board.application.mapper.NoticeMapper;
import com.weeth.domain.board.domain.entity.Notice;
import com.weeth.domain.board.domain.service.NoticeDeleteService;
import com.weeth.domain.board.domain.service.NoticeFindService;
import com.weeth.domain.board.domain.service.NoticeSaveService;
import com.weeth.domain.board.domain.service.NoticeUpdateService;
import com.weeth.domain.comment.application.dto.response.CommentResponse;
import com.weeth.domain.comment.application.usecase.query.GetCommentQueryService;
import com.weeth.domain.comment.domain.entity.Comment;
import com.weeth.domain.file.application.dto.response.FileResponse;
import com.weeth.domain.file.application.mapper.FileMapper;
import com.weeth.domain.file.domain.entity.File;
import com.weeth.domain.file.domain.entity.FileOwnerType;
import com.weeth.domain.file.domain.repository.FileReader;
import com.weeth.domain.file.domain.repository.FileRepository;
import com.weeth.domain.user.application.exception.UserNotMatchException;
import com.weeth.domain.user.domain.entity.User;
import com.weeth.domain.user.domain.service.UserGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeUsecaseImpl implements NoticeUsecase {

    private final NoticeSaveService noticeSaveService;
    private final NoticeFindService noticeFindService;
    private final NoticeUpdateService noticeUpdateService;
    private final NoticeDeleteService noticeDeleteService;

    private final UserGetService userGetService;

    private final FileRepository fileRepository;
    private final FileReader fileReader;

    private final NoticeMapper mapper;
    private final GetCommentQueryService getCommentQueryService;
    private final FileMapper fileMapper;

    @Override
    @Transactional
    public NoticeDTO.SaveResponse save(NoticeDTO.Save request, Long userId) {
        User user = userGetService.find(userId);

        Notice notice = mapper.fromNoticeDto(request, user);
        Notice savedNotice = noticeSaveService.save(notice);

        List<File> files = fileMapper.toFileList(request.files(), FileOwnerType.NOTICE, savedNotice.getId());
        fileRepository.saveAll(files);

        return mapper.toSaveResponse(savedNotice);
    }

    @Override
    public NoticeDTO.Response findNotice(Long noticeId) {
        Notice notice = noticeFindService.find(noticeId);

        List<FileResponse> response = getFiles(noticeId).stream()
                .map(fileMapper::toFileResponse)
                .toList();

        return mapper.toNoticeDto(notice, response, filterParentComments(notice.getComments()));
    }

    @Override
    public Slice<NoticeDTO.ResponseAll> findNotices(int pageNumber, int pageSize) {
        if (pageNumber < 0) {
            throw new PageNotFoundException();
        }
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "id")); // id를 기준으로 내림차순
        Slice<Notice> notices = noticeFindService.findRecentNotices(pageable);
        return notices.map(notice->mapper.toAll(notice, checkFileExistsByNotice(notice.id)));
    }

    @Override
    public Slice<NoticeDTO.ResponseAll> searchNotice(String keyword, int pageNumber, int pageSize) {
        validatePageNumber(pageNumber);

        keyword = keyword.strip();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Slice<Notice> notices = noticeFindService.search(keyword, pageable);

        if (notices.isEmpty()){
            throw new NoSearchResultException();
        }

        return notices.map(notice -> mapper.toAll(notice, checkFileExistsByNotice(notice.id)));
    }

    @Override
    @Transactional
    public NoticeDTO.SaveResponse update(Long noticeId, NoticeDTO.Update dto, Long userId) {
        Notice notice = validateOwner(noticeId, userId);

        if (dto.files() != null) {
            List<File> fileList = getFiles(noticeId);
            fileRepository.deleteAll(fileList);

            List<File> files = fileMapper.toFileList(dto.files(), FileOwnerType.NOTICE, notice.getId());
            fileRepository.saveAll(files);
        }

        noticeUpdateService.update(notice, dto);

        return mapper.toSaveResponse(notice);
    }

    @Override
    @Transactional
    public void delete(Long noticeId, Long userId) {
        validateOwner(noticeId, userId);

        List<File> fileList = getFiles(noticeId);
        fileRepository.deleteAll(fileList);

        noticeDeleteService.delete(noticeId);
    }

    private List<File> getFiles(Long noticeId) {
        return fileReader.findAll(FileOwnerType.NOTICE, noticeId, null);
    }

    private Notice validateOwner(Long noticeId, Long userId) {
        Notice notice = noticeFindService.find(noticeId);
        if (!notice.getUser().getId().equals(userId)) {
            throw new UserNotMatchException();
        }
        return notice;
    }

    private boolean checkFileExistsByNotice(Long noticeId){
        return fileReader.exists(FileOwnerType.NOTICE, noticeId, null);
    }

    private List<CommentResponse> filterParentComments(List<Comment> comments) {
        return getCommentQueryService.toCommentTreeResponses(comments);
    }

    private void validatePageNumber(int pageNumber){
        if (pageNumber < 0) {
            throw new PageNotFoundException();
        }
    }
}
