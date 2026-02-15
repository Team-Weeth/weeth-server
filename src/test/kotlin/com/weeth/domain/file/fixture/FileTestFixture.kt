package com.weeth.domain.file.fixture

import com.weeth.domain.board.domain.entity.Notice
import com.weeth.domain.file.domain.entity.File

object FileTestFixture {
    fun createFile(
        id: Long,
        fileName: String,
        fileUrl: String,
        notice: Notice? = null,
    ): File =
        File
            .builder()
            .id(id)
            .fileName(fileName)
            .fileUrl(fileUrl)
            .notice(notice)
            .build()
}
