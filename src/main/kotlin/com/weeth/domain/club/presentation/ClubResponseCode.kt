package com.weeth.domain.club.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class ClubResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    CLUB_CREATED_SUCCESS(11100, HttpStatus.CREATED, "동아리가 성공적으로 생성되었습니다."),
    CLUB_FIND_ALL_SUCCESS(11101, HttpStatus.OK, "동아리 목록을 성공적으로 조회했습니다."),
    CLUB_FIND_BY_ID_SUCCESS(11102, HttpStatus.OK, "동아리 정보를 성공적으로 조회했습니다."),
    CLUB_UPDATED_SUCCESS(11103, HttpStatus.OK, "동아리 정보가 성공적으로 수정되었습니다."),
    CLUB_CODE_REGENERATED_SUCCESS(11104, HttpStatus.OK, "초대 코드가 재생성되었습니다."),
    CLUB_JOINED_SUCCESS(11105, HttpStatus.OK, "동아리에 가입했습니다."),
    CLUB_LEFT_SUCCESS(11106, HttpStatus.OK, "동아리를 탈퇴했습니다."),
    MEMBER_FIND_ALL_SUCCESS(11107, HttpStatus.OK, "동아리 멤버 목록을 성공적으로 조회했습니다."),
    MEMBER_FIND_ME_SUCCESS(11108, HttpStatus.OK, "내 멤버 정보를 성공적으로 조회했습니다."),
    MEMBER_ACCEPTED_SUCCESS(11109, HttpStatus.OK, "멤버가 승인되었습니다."),
    MEMBER_BANNED_SUCCESS(11110, HttpStatus.OK, "멤버가 추방되었습니다."),
    MEMBER_ROLE_UPDATED_SUCCESS(11111, HttpStatus.OK, "멤버 권한이 변경되었습니다."),
    CLUB_FIND_SUCCESS(11112, HttpStatus.OK, "동아리 공개 정보를 성공적으로 조회했습니다."),
    CLUB_PROFILE_IMAGE_DELETED_SUCCESS(11113, HttpStatus.OK, "동아리 프로필 사진이 삭제되었습니다."),
    CLUB_BACKGROUND_IMAGE_DELETED_SUCCESS(11114, HttpStatus.OK, "동아리 배경 사진이 삭제되었습니다."),
    MEMBER_APPLY_OB_SUCCESS(11115, HttpStatus.OK, "멤버의 OB 기수 등록이 완료되었습니다."),
    MEMBER_PROFILE_IMAGE_DELETED_SUCCESS(11118, HttpStatus.OK, "동아리 프로필 사진이 삭제되었습니다."),
    MEMBER_PROFILE_UPDATED_SUCCESS(11119, HttpStatus.OK, "프로필이 성공적으로 수정되었습니다."),
}
