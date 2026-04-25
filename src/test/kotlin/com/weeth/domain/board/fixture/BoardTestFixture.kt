package com.weeth.domain.board.fixture

import com.weeth.domain.board.domain.entity.Board
import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.vo.BoardConfig
import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.fixture.ClubTestFixture
import org.springframework.test.util.ReflectionTestUtils

object BoardTestFixture {
    fun create(
        id: Long = 0L,
        club: Club = ClubTestFixture.createClub(),
        name: String = "일반 게시판",
        description: String = "일반 게시판 설명",
        type: BoardType = BoardType.GENERAL,
        config: BoardConfig = BoardConfig(),
    ): Board {
        val board =
            Board(
                club = club,
                name = name,
                description = description,
                type = type,
                config = config,
            )
        if (id != 0L) ReflectionTestUtils.setField(board, "id", id)
        return board
    }

    fun createNoticeBoard(
        club: Club = ClubTestFixture.createClub(),
        name: String = "공지사항",
        description: String = "공지사항 게시판 설명",
    ): Board =
        create(
            club = club,
            name = name,
            description = description,
            type = BoardType.NOTICE,
            config = BoardConfig(writePermission = MemberRole.ADMIN),
        )
}
