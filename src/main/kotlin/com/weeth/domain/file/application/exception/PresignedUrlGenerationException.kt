package com.weeth.domain.file.application.exception

import com.weeth.global.common.exception.BaseException

class PresignedUrlGenerationException(
    cause: Throwable? = null,
) : BaseException(FileErrorCode.PRESIGNED_URL_GENERATION_FAILED, cause)
