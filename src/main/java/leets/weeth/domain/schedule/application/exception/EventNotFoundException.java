package leets.weeth.domain.schedule.application.exception;

import leets.weeth.global.common.exception.BusinessLogicException;

public class EventNotFoundException extends BusinessLogicException {
    public EventNotFoundException() {
        super(EventErrorCode.EVENT_NOT_FOUND);
    }
}
