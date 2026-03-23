package com.weeth.domain.university.presentation

import com.weeth.global.common.response.ResponseCodeInterface
import org.springframework.http.HttpStatus

enum class UniversityResponseCode(
    override val code: Int,
    override val status: HttpStatus,
    override val message: String,
) : ResponseCodeInterface {
    SCHOOL_FIND_ALL_SUCCESS(11300, HttpStatus.OK, "학교 목록을 성공적으로 조회했습니다."),
    MAJOR_FIND_ALL_SUCCESS(11301, HttpStatus.OK, "학과 목록을 성공적으로 조회했습니다."),
}
