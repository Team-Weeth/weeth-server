package com.weeth.domain.user.application.dto.response

import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.domain.user.domain.entity.enums.Status
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class AdminUserResponse(
    @field:Schema(description = "사용자 ID", example = "1")
    val id: Long,
    @field:Schema(description = "이름", example = "홍길동")
    val name: String,
    @field:Schema(description = "이메일", example = "hong@example.com")
    val email: String,
    @field:Schema(description = "학번", example = "20201234")
    val studentId: String,
    @field:Schema(description = "전화번호", example = "01012345678")
    val tel: String,
    @field:Schema(description = "학과", example = "컴퓨터공학과")
    val department: String,
    @field:Schema(description = "소속 기수 목록", example = "[6, 7]")
    val cardinals: List<Int>,
    @field:Schema(description = "회원 상태", example = "ACTIVE")
    val status: Status,
    @field:Schema(description = "권한", example = "USER", nullable = true)
    val role: Role?,
    @field:Schema(description = "출석 횟수", example = "8")
    val attendanceCount: Int,
    @field:Schema(description = "결석 횟수", example = "2")
    val absenceCount: Int,
    @field:Schema(description = "출석률", example = "80")
    val attendanceRate: Int,
    @field:Schema(description = "패널티 횟수", example = "1")
    val penaltyCount: Int,
    @field:Schema(description = "경고 횟수", example = "0")
    val warningCount: Int,
    @field:Schema(description = "생성 시각")
    val createdAt: LocalDateTime?,
    @field:Schema(description = "수정 시각")
    val modifiedAt: LocalDateTime?,
)
