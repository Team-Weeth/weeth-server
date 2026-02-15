package com.weeth.domain.board.presentation;
import com.weeth.global.common.response.ResponseCodeInterface;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BoardResponseCode implements ResponseCodeInterface {
    //NoticeAdminController 관련
    NOTICE_CREATED_SUCCESS(1300, HttpStatus.OK, "공지사항이 성공적으로 생성되었습니다."),
    NOTICE_UPDATED_SUCCESS(1301, HttpStatus.OK, "공지사항이 성공적으로 수정되었습니다."),
    NOTICE_DELETED_SUCCESS(1302, HttpStatus.OK, "공지사항이 성공적으로 삭제되었습니다."),
    //NoticeController 관련
    NOTICE_FIND_ALL_SUCCESS(1303, HttpStatus.OK, "공지사항 목록이 성공적으로 조회되었습니다."),
    NOTICE_FIND_BY_ID_SUCCESS(1304, HttpStatus.OK, "공지사항이 성공적으로 조회되었습니다."),
    NOTICE_SEARCH_SUCCESS(1305, HttpStatus.OK, "공지사항 검색 결과가 성공적으로 조회되었습니다."),
    //PostController 관련
    POST_CREATED_SUCCESS(1306, HttpStatus.OK, "게시글이 성공적으로 생성되었습니다."),
    POST_UPDATED_SUCCESS(1307, HttpStatus.OK, "파트 게시글이 성공적으로 수정되었습니다."),
    POST_DELETED_SUCCESS(1308, HttpStatus.OK, "게시글이 성공적으로 삭제되었습니다."),
    POST_FIND_ALL_SUCCESS(1309, HttpStatus.OK, "게시글 목록이 성공적으로 조회되었습니다."),
    POST_PART_FIND_ALL_SUCCESS(1310, HttpStatus.OK, "파트별 게시글 목록이 성공적으로 조회되었습니다."),
    POST_EDU_FIND_SUCCESS(1311, HttpStatus.OK, "교육 게시글 목록이 성공적으로 조회되었습니다."),
    POST_FIND_BY_ID_SUCCESS(1312, HttpStatus.OK, "파트 게시글이 성공적으로 조회되었습니다."),
    POST_SEARCH_SUCCESS(1313, HttpStatus.OK, "파트 게시글 검색 결과가 성공적으로 조회되었습니다."),
    EDUCATION_SEARCH_SUCCESS(1314, HttpStatus.OK, "교육 자료 검색 결과가 성공적으로 조회되었습니다."),
    POST_STUDY_NAMES_FIND_SUCCESS(1315, HttpStatus.OK, "스터디 이름 목록이 성공적으로 조회되었습니다."),

    EDUCATION_UPDATED_SUCCESS(1316, HttpStatus.OK, "교육자료가 성공적으로 수정되었습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;

    BoardResponseCode(int code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }
}
