package com.weeth.domain.releasenote.application.exception

import com.weeth.global.common.exception.BaseException

class NotDeveloperException : BaseException(ReleaseNoteErrorCode.RELEASE_NOTE_FORBIDDEN)
