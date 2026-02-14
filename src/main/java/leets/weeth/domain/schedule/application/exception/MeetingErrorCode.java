package leets.weeth.domain.schedule.application.exception;

import leets.weeth.global.common.exception.ErrorCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MeetingErrorCode implements ErrorCodeInterface {

    MEETING_NOT_FOUND(404, HttpStatus.NOT_FOUND, "존재하지 않는 정기모임입니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
