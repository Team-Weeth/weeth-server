package com.weeth.domain.schedule.application.exception;

import com.weeth.global.common.exception.BaseException;

public class EventNotFoundException extends BaseException {
    public EventNotFoundException() {
        super(EventErrorCode.EVENT_NOT_FOUND);
    }
}
