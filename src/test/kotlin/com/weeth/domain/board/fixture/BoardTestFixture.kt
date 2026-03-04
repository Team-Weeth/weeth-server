package com.weeth.domain.board.fixture

import com.weeth.domain.board.domain.entity.Board
import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.vo.BoardConfig
import com.weeth.domain.user.domain.enums.Role

object BoardTestFixture {
    fun create(
        name: String = "일반 게시판",
        type: BoardType = BoardType.GENERAL,
        config: BoardConfig = BoardConfig(),
    ): Board =
        Board(
            name = name,
            type = type,
            config = config,
        )

    fun createNoticeBoard(name: String = "공지사항"): Board =
        create(
            name = name,
            type = BoardType.NOTICE,
            config = BoardConfig(writePermission = Role.ADMIN),
        )
}
