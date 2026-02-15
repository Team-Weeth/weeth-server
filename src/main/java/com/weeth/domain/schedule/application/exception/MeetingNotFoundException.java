package com.weeth.domain.schedule.application.exception;

import com.weeth.global.common.exception.BusinessLogicException;

public class MeetingNotFoundException extends BusinessLogicException {
    public MeetingNotFoundException() {super(MeetingErrorCode.MEETING_NOT_FOUND);}
}
