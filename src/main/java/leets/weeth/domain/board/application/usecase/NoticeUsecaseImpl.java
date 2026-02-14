package leets.weeth.domain.board.application.usecase;

import leets.weeth.domain.board.application.dto.NoticeDTO;
import leets.weeth.domain.board.application.exception.NoSearchResultException;
import leets.weeth.domain.board.application.exception.PageNotFoundException;
import leets.weeth.domain.board.application.mapper.NoticeMapper;
import leets.weeth.domain.board.domain.entity.Notice;
import leets.weeth.domain.board.domain.service.NoticeDeleteService;
import leets.weeth.domain.board.domain.service.NoticeFindService;
import leets.weeth.domain.board.domain.service.NoticeSaveService;
import leets.weeth.domain.board.domain.service.NoticeUpdateService;
import leets.weeth.domain.comment.application.dto.CommentDTO;
import leets.weeth.domain.comment.application.mapper.CommentMapper;
import leets.weeth.domain.comment.domain.entity.Comment;
import leets.weeth.domain.file.application.dto.response.FileResponse;
import leets.weeth.domain.file.application.mapper.FileMapper;
import leets.weeth.domain.file.domain.entity.File;
import leets.weeth.domain.file.domain.service.FileDeleteService;
import leets.weeth.domain.file.domain.service.FileGetService;
import leets.weeth.domain.file.domain.service.FileSaveService;
import leets.weeth.domain.user.application.exception.UserNotMatchException;
import leets.weeth.domain.user.domain.entity.User;
import leets.weeth.domain.user.domain.service.UserGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeUsecaseImpl implements NoticeUsecase {

    private final NoticeSaveService noticeSaveService;
    private final NoticeFindService noticeFindService;
    private final NoticeUpdateService noticeUpdateService;
    private final NoticeDeleteService noticeDeleteService;

    private final UserGetService userGetService;

    private final FileSaveService fileSaveService;
    private final FileGetService fileGetService;
    private final FileDeleteService fileDeleteService;

    private final NoticeMapper mapper;
    private final CommentMapper commentMapper;
    private final FileMapper fileMapper;

    @Override
    @Transactional
    public NoticeDTO.SaveResponse save(NoticeDTO.Save request, Long userId) {
        User user = userGetService.find(userId);

        Notice notice = mapper.fromNoticeDto(request, user);
        Notice savedNotice = noticeSaveService.save(notice);

        List<File> files = fileMapper.toFileList(request.files(), notice);
        fileSaveService.save(files);

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

        List<File> fileList = getFiles(noticeId);
        fileDeleteService.delete(fileList);

        List<File> files = fileMapper.toFileList(dto.files(), notice);
        fileSaveService.save(files);

        noticeUpdateService.update(notice, dto);

        return mapper.toSaveResponse(notice);
    }

    @Override
    @Transactional
    public void delete(Long noticeId, Long userId) {
        validateOwner(noticeId, userId);

        List<File> fileList = getFiles(noticeId);
        fileDeleteService.delete(fileList);

        noticeDeleteService.delete(noticeId);
    }

    private List<File> getFiles(Long noticeId) {
        return fileGetService.findAllByNotice(noticeId);
    }

    private Notice validateOwner(Long noticeId, Long userId) {
        Notice notice = noticeFindService.find(noticeId);
        if (!notice.getUser().getId().equals(userId)) {
            throw new UserNotMatchException();
        }
        return notice;
    }

    private boolean checkFileExistsByNotice(Long noticeId){
        return !fileGetService.findAllByNotice(noticeId).isEmpty();
    }

    private List<CommentDTO.Response> filterParentComments(List<Comment> comments) {
        Map<Long, List<Comment>> commentMap = comments.stream()
                .filter(comment -> comment.getParent() != null)
                .collect(Collectors.groupingBy(comment -> comment.getParent().getId()));

        return comments.stream()
                .filter(comment -> comment.getParent() == null) // 부모 댓글만 가져오기
                .map(parent -> mapToDtoWithChildren(parent, commentMap))
                .toList();
    }

    private CommentDTO.Response mapToDtoWithChildren(Comment comment, Map<Long, List<Comment>> commentMap) {
        List<CommentDTO.Response> children = commentMap.getOrDefault(comment.getId(), Collections.emptyList())
                .stream()
                .map(child -> mapToDtoWithChildren(child, commentMap))
                .collect(Collectors.toList());

        List<FileResponse> files = fileGetService.findAllByComment(comment.getId()).stream()
                .map(fileMapper::toFileResponse)
                .toList();

        return commentMapper.toCommentDto(comment, children, files);
    }

    private void validatePageNumber(int pageNumber){
        if (pageNumber < 0) {
            throw new PageNotFoundException();
        }
    }
}
