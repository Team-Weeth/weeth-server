package com.weeth.global.common.controller

import com.weeth.domain.account.application.exception.AccountErrorCode
import com.weeth.domain.attendance.application.exception.AttendanceErrorCode
import com.weeth.domain.attendance.application.exception.SessionErrorCode
import com.weeth.domain.board.application.exception.BoardErrorCode
import com.weeth.domain.comment.application.exception.CommentErrorCode
import com.weeth.domain.penalty.application.exception.PenaltyErrorCode
import com.weeth.domain.schedule.application.exception.EventErrorCode
import com.weeth.domain.user.application.exception.UserErrorCode
import com.weeth.global.auth.jwt.application.exception.JwtErrorCode
import com.weeth.global.common.exception.ApiErrorCodeExample
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v4/docs/exceptions")
@Tag(name = "Exception Document", description = "API 에러 코드 문서")
class ExceptionDocController {
    @GetMapping("/account")
    @Operation(summary = "Account 도메인 에러 코드 목록")
    @ApiErrorCodeExample(AccountErrorCode::class)
    fun accountErrorCodes() {
    }

    @GetMapping("/attendance")
    @Operation(summary = "Attendance 도메인 에러 코드 목록")
    @ApiErrorCodeExample(AttendanceErrorCode::class, SessionErrorCode::class)
    fun attendanceErrorCodes() {
    }

    @GetMapping("/board")
    @Operation(summary = "Board 도메인 에러 코드 목록")
    @ApiErrorCodeExample(BoardErrorCode::class, CommentErrorCode::class)
    fun boardErrorCodes() {
    }

    @GetMapping("/penalty")
    @Operation(summary = "Penalty 도메인 에러 코드 목록")
    @ApiErrorCodeExample(PenaltyErrorCode::class)
    fun penaltyErrorCodes() {
    }

    @GetMapping("/schedule")
    @Operation(summary = "Schedule 도메인 에러 코드 목록")
    @ApiErrorCodeExample(EventErrorCode::class)
    fun scheduleErrorCodes() {
    }

    @GetMapping("/user")
    @Operation(summary = "User 도메인 에러 코드 목록")
    @ApiErrorCodeExample(UserErrorCode::class)
    fun userErrorCodes() {
    }

    // todo: SAS 관련 예외도 추가
    @GetMapping("/auth")
    @Operation(summary = "인증/인가 에러 코드 목록")
    @ApiErrorCodeExample(JwtErrorCode::class)
    fun authErrorCodes() {
    }
}
