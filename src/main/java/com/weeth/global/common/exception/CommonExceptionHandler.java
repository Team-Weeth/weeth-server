package com.weeth.global.common.exception;

import com.weeth.global.common.response.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class CommonExceptionHandler {

    private static final String INPUT_FORMAT_ERROR_MESSAGE = "입력 포맷이 올바르지 않습니다.";
    private static final String LOG_FORMAT = "Class : {}, Code : {}, Message : {}";

    @ExceptionHandler(BusinessLogicException.class)  // 커스텀 예외 처리
    public ResponseEntity<CommonResponse<Void>> handle(BusinessLogicException ex) {
        log.warn("구체로그: ", ex);
        log.warn(LOG_FORMAT, ex.getClass().getSimpleName(), ex.getStatusCode(), ex.getMessage());

        CommonResponse<Void> response = CommonResponse.createFailure(ex.getStatusCode(), ex.getMessage());

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(response);
    }

    @ExceptionHandler(BindException.class)  // BindException == @ModelAttribute 어노테이션으로 받은 파라미터의 @Valid 통해 발생한 Exception
    public ResponseEntity<CommonResponse<List<BindExceptionResponse>>> handle(BindException ex) {
        int statusCode = 400;
        List<BindExceptionResponse> exceptionResponses = new ArrayList<>();

        if (ex instanceof ErrorResponse) {
            statusCode = ((ErrorResponse) ex).getStatusCode().value();
            ex.getBindingResult().getFieldErrors().forEach(fieldError -> {
                exceptionResponses.add(BindExceptionResponse.builder()
                        .message(fieldError.getDefaultMessage())
                        .value(fieldError.getRejectedValue())
                        .build());
            });
        }

        log.warn("구체로그: ", ex);
        log.warn(LOG_FORMAT, ex.getClass().getSimpleName(), statusCode, exceptionResponses);

        CommonResponse<List<BindExceptionResponse>> response = CommonResponse.createFailure(statusCode, "bindException", exceptionResponses);

        return ResponseEntity
                .status(statusCode)
                .body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    // MethodArgumentTypeMismatchException == 클라이언트가 날짜 포맷을 다르게 입력한 경우
    public ResponseEntity<CommonResponse<Void>> handle(MethodArgumentTypeMismatchException ex) {
        int statusCode = 400;   // 파라미터 값 실수이므로 4XX

        if (ex instanceof ErrorResponse) {   // Exception이 ErrorResponse의 인스턴스라면
            statusCode = ((ErrorResponse) ex).getStatusCode().value();   // ErrorResponse에서 상태 값 가져오기
        }

        log.warn("구체로그: ", ex);
        log.warn(LOG_FORMAT, ex.getClass().getSimpleName(), statusCode, ex.getMessage());

        CommonResponse<Void> response = CommonResponse.createFailure(statusCode, INPUT_FORMAT_ERROR_MESSAGE);

        return ResponseEntity
                .status(statusCode)
                .body(response);
    }

    @ExceptionHandler(Exception.class)  // 모든 Exception 처리
    public ResponseEntity<CommonResponse<Void>> handle(Exception ex) {
        int statusCode = 500;

        if (ex instanceof ErrorResponse) {   // Exception이 ErrorResponse의 인스턴스라면 (http status를 가지는 예외)
            statusCode = ((ErrorResponse) ex).getStatusCode().value();   // ErrorResponse에서 상태 값 가져오기
        }

        log.warn("구체로그: ", ex);
        log.warn(LOG_FORMAT, ex.getClass().getSimpleName(), statusCode, ex.getMessage());

        CommonResponse<Void> response = CommonResponse.createFailure(statusCode, ex.getMessage());

        return ResponseEntity
                .status(statusCode)
                .body(response);
    }
}