package com.weeth.domain.board.fixture

import com.weeth.domain.board.domain.entity.Board
import com.weeth.domain.board.domain.entity.enums.BoardType
import com.weeth.domain.board.domain.vo.BoardConfig
import com.weeth.domain.user.domain.entity.enums.Role

object BoardTestFixture {
    fun create(
        id: Long = 1L,
        name: String = "일반 게시판",
        type: BoardType = BoardType.GENERAL,
        config: BoardConfig = BoardConfig(),
    ): Board =
        Board(
            id = id,
            name = name,
            type = type,
            config = config,
        )

    fun createNoticeBoard(
        id: Long = 2L,
        name: String = "공지사항",
    ): Board =
        create(
            id = id,
            name = name,
            type = BoardType.NOTICE,
            config = BoardConfig(writePermission = Role.ADMIN),
        )
}
