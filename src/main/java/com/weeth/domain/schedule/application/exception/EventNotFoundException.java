package com.weeth.domain.schedule.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class EventNotFoundException extends BusinessLogicException {
    public EventNotFoundException() {
        super(EventErrorCode.EVENT_NOT_FOUND);
    }
}
