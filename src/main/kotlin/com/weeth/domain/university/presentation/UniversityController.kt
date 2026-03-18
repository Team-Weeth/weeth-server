package com.weeth.domain.university.presentation

import com.weeth.domain.university.application.dto.response.MajorResponse
import com.weeth.domain.university.application.dto.response.SchoolResponse
import com.weeth.domain.university.application.exception.UniversityErrorCode
import com.weeth.domain.university.application.usecase.query.GetMajorQueryService
import com.weeth.domain.university.application.usecase.query.GetSchoolQueryService
import com.weeth.domain.university.presentation.UniversityResponseCode.MAJOR_FIND_ALL_SUCCESS
import com.weeth.domain.university.presentation.UniversityResponseCode.SCHOOL_FIND_ALL_SUCCESS
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "UNIVERSITY", description = "학교/학과 API")
@RestController
@RequestMapping("/api/v4/university")
@ApiErrorCodeExample(UniversityErrorCode::class)
class UniversityController(
    private val getSchoolQueryService: GetSchoolQueryService,
    private val getMajorQueryService: GetMajorQueryService,
) {
    @GetMapping("/schools")
    @Operation(summary = "학교 목록 조회")
    fun getSchools(): CommonResponse<List<SchoolResponse>> =
        CommonResponse.success(SCHOOL_FIND_ALL_SUCCESS, getSchoolQueryService.getSchools())

    @GetMapping("/majors")
    @Operation(summary = "학과 목록 조회")
    fun getMajors(): CommonResponse<List<MajorResponse>> =
        CommonResponse.success(MAJOR_FIND_ALL_SUCCESS, getMajorQueryService.getMajors())
}
