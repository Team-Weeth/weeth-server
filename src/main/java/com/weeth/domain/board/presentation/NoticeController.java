package com.weeth.domain.board.presentation;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.weeth.domain.board.application.dto.NoticeDTO;
import com.weeth.domain.board.application.exception.BoardErrorCode;
import com.weeth.domain.board.application.exception.NoticeErrorCode;
import com.weeth.domain.board.application.usecase.NoticeUsecase;
import com.weeth.global.common.exception.ApiErrorCodeExample;
import com.weeth.global.common.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.*;

import static com.weeth.domain.board.presentation.BoardResponseCode.*;


@Tag(name = "NOTICE", description = "공지사항 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notices")
@ApiErrorCodeExample({BoardErrorCode.class, NoticeErrorCode.class})
public class NoticeController {

    private final NoticeUsecase noticeUsecase;

    @GetMapping
    @Operation(summary="공지사항 목록 조회 [무한스크롤]")
    public CommonResponse<Slice<NoticeDTO.ResponseAll>> findNotices(@RequestParam("pageNumber") int pageNumber, @RequestParam("pageSize") int pageSize) {
        return CommonResponse.success(NOTICE_FIND_ALL_SUCCESS, noticeUsecase.findNotices(pageNumber, pageSize));
    }

    @GetMapping("/{noticeId}")
    @Operation(summary="특정 공지사항 조회")
    public CommonResponse<NoticeDTO.Response> findNoticeById(@PathVariable Long noticeId) {
        return CommonResponse.success(NOTICE_FIND_BY_ID_SUCCESS, noticeUsecase.findNotice(noticeId));
    }

    @GetMapping("/search")
    @Operation(summary="공지사항 검색 [무한스크롤]")
    public CommonResponse<Slice<NoticeDTO.ResponseAll>> findNotice(@RequestParam String keyword, @RequestParam("pageNumber") int pageNumber, @RequestParam("pageSize") int pageSize) {
        return CommonResponse.success(NOTICE_SEARCH_SUCCESS, noticeUsecase.searchNotice(keyword, pageNumber, pageSize));
    }
}
