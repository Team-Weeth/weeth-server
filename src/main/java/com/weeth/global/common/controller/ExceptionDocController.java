package com.weeth.global.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.weeth.domain.account.application.exception.AccountErrorCode;
import com.weeth.domain.attendance.application.exception.AttendanceErrorCode;
import com.weeth.domain.board.application.exception.BoardErrorCode;
import com.weeth.domain.comment.application.exception.CommentErrorCode;
import com.weeth.domain.penalty.application.exception.PenaltyErrorCode;
import com.weeth.domain.schedule.application.exception.EventErrorCode;
import com.weeth.domain.schedule.application.exception.MeetingErrorCode;
import com.weeth.domain.user.application.exception.UserErrorCode;
import com.weeth.global.auth.jwt.exception.JwtErrorCode;
import com.weeth.global.common.exception.ApiErrorCodeExample;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/docs/exceptions")
@Tag(name = "Exception Document", description = "API 에러 코드 문서")
public class ExceptionDocController {

    @GetMapping("/account")
    @Operation(summary = "Account 도메인 에러 코드 목록")
    @ApiErrorCodeExample(AccountErrorCode.class)
    public void accountErrorCodes() {
    }

    @GetMapping("/attendance")
    @Operation(summary = "Attendance 도메인 에러 코드 목록")
    @ApiErrorCodeExample(AttendanceErrorCode.class)
    public void attendanceErrorCodes() {
    }

    @GetMapping("/board")
    @Operation(summary = "Board 도메인 에러 코드 목록")
    @ApiErrorCodeExample({BoardErrorCode.class, CommentErrorCode.class})
    public void boardErrorCodes() {
    }

    @GetMapping("/penalty")
    @Operation(summary = "Penalty 도메인 에러 코드 목록")
    @ApiErrorCodeExample(PenaltyErrorCode.class)
    public void penaltyErrorCodes() {
    }

    @GetMapping("/schedule")
    @Operation(summary = "Schedule 도메인 에러 코드 목록")
    @ApiErrorCodeExample({EventErrorCode.class, MeetingErrorCode.class})
    public void scheduleErrorCodes() {
    }

    @GetMapping("/user")
    @Operation(summary = "User 도메인 에러 코드 목록")
    @ApiErrorCodeExample(UserErrorCode.class)
    public void userErrorCodes() {
    }

    @GetMapping("/auth")
    @Operation(summary = "인증/인가 에러 코드 목록")
    @ApiErrorCodeExample({JwtErrorCode.class})
    public void authErrorCodes() {
    }
}
