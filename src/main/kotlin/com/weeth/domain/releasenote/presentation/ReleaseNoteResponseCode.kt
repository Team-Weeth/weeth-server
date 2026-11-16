package com.weeth.domain.releasenote.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class ReleaseNoteResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    RELEASE_NOTE_LIST_SUCCESS(11600, HttpStatus.OK, "릴리즈 노트 목록 조회 성공"),
    RELEASE_NOTE_DETAIL_SUCCESS(11601, HttpStatus.OK, "릴리즈 노트 상세 조회 성공"),
    RELEASE_NOTE_ADMIN_LIST_SUCCESS(11602, HttpStatus.OK, "릴리즈 노트 백오피스 목록 조회 성공"),
    RELEASE_NOTE_ADMIN_DETAIL_SUCCESS(11603, HttpStatus.OK, "릴리즈 노트 백오피스 상세 조회 성공"),
    RELEASE_NOTE_CREATED_SUCCESS(11604, HttpStatus.OK, "릴리즈 노트 작성 성공"),
    RELEASE_NOTE_UPDATED_SUCCESS(11605, HttpStatus.OK, "릴리즈 노트 수정 성공"),
    RELEASE_NOTE_STATUS_UPDATED_SUCCESS(11606, HttpStatus.OK, "릴리즈 노트 상태 변경 성공"),
    RELEASE_NOTE_DELETED_SUCCESS(11607, HttpStatus.OK, "릴리즈 노트 삭제 성공"),
}
