package com.weeth.domain.schedule.application.exception;

import com.weeth.global.common.exception.BaseException;

public class MeetingNotFoundException extends BaseException {
    public MeetingNotFoundException() {super(MeetingErrorCode.MEETING_NOT_FOUND);}
}
