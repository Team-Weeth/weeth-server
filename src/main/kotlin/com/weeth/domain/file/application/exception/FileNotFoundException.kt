package com.weeth.domain.file.application.exception

import com.weeth.global.common.exception.BaseException

class FileNotFoundException : BaseException(FileErrorCode.FILE_NOT_FOUND)
