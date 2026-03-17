package com.weeth.domain.board.fixture

import com.weeth.domain.board.domain.entity.Board
import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.vo.BoardConfig
import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.fixture.ClubTestFixture

object BoardTestFixture {
    fun create(
        club: Club = ClubTestFixture.createClub(),
        name: String = "일반 게시판",
        type: BoardType = BoardType.GENERAL,
        config: BoardConfig = BoardConfig(),
    ): Board =
        Board(
            club = club,
            name = name,
            type = type,
            config = config,
        )

    fun createNoticeBoard(
        club: Club = ClubTestFixture.createClub(),
        name: String = "공지사항",
    ): Board =
        create(
            club = club,
            name = name,
            type = BoardType.NOTICE,
            config = BoardConfig(writePermission = MemberRole.ADMIN),
        )
}
