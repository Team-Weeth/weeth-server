package com.weeth.domain.releasenote.application.exception

import com.weeth.global.common.exception.BaseException

class ReleaseNoteNotFoundException : BaseException(ReleaseNoteErrorCode.RELEASE_NOTE_NOT_FOUND)
