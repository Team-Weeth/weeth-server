package com.weeth.domain.board.presentation;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseMessage {
    //NoticeAdminController 관련
    NOTICE_CREATED_SUCCESS("공지사항이 성공적으로 생성되었습니다."),
    NOTICE_UPDATED_SUCCESS("공지사항이 성공적으로 수정되었습니다."),
    NOTICE_DELETED_SUCCESS("공지사항이 성공적으로 삭제되었습니다."),
    //NoticeController 관련
    NOTICE_FIND_ALL_SUCCESS("공지사항 목록이 성공적으로 조회되었습니다."),
    NOTICE_FIND_BY_ID_SUCCESS("공지사항이 성공적으로 조회되었습니다."),
    NOTICE_SEARCH_SUCCESS("공지사항 검색 결과가 성공적으로 조회되었습니다."),
    //PostController 관련
    POST_CREATED_SUCCESS("게시글이 성공적으로 생성되었습니다."),
    POST_UPDATED_SUCCESS("파트 게시글이 성공적으로 수정되었습니다."),
    POST_DELETED_SUCCESS("게시글이 성공적으로 삭제되었습니다."),
    POST_FIND_ALL_SUCCESS("게시글 목록이 성공적으로 조회되었습니다."),
    POST_PART_FIND_ALL_SUCCESS("파트별 게시글 목록이 성공적으로 조회되었습니다."),
    POST_EDU_FIND_SUCCESS("교육 게시글 목록이 성공적으로 조회되었습니다."),
    POST_FIND_BY_ID_SUCCESS("파트 게시글이 성공적으로 조회되었습니다."),
    POST_SEARCH_SUCCESS("파트 게시글 검색 결과가 성공적으로 조회되었습니다."),
    EDUCATION_SEARCH_SUCCESS("교육 자료 검색 결과가 성공적으로 조회되었습니다."),
    POST_STUDY_NAMES_FIND_SUCCESS("스터디 이름 목록이 성공적으로 조회되었습니다."),

    EDUCATION_UPDATED_SUCCESS("교육자료가 성공적으로 수정되었습니다.");

    private final String message;
}
