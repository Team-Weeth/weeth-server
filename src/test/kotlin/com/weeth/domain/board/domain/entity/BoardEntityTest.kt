package com.weeth.domain.board.domain.entity

import com.weeth.domain.board.domain.entity.enums.BoardType
import com.weeth.domain.board.domain.vo.BoardConfig
import com.weeth.domain.user.domain.entity.enums.Role
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class BoardEntityTest :
    StringSpec({
        "isCommentEnabled는 config 값을 반영한다" {
            val board =
                Board(
                    id = 1L,
                    name = "공지사항",
                    type = BoardType.NOTICE,
                    config = BoardConfig(commentEnabled = false),
                )

            board.isCommentEnabled shouldBe false
        }

        "rename은 빈 이름이면 예외를 던진다" {
            val board = Board(id = 1L, name = "게시판", type = BoardType.GENERAL)

            shouldThrow<IllegalArgumentException> {
                board.rename(" ")
            }
        }

        "isAdminOnly는 writePermission이 ADMIN일 때 true를 반환한다" {
            val board =
                Board(
                    id = 2L,
                    name = "공지",
                    type = BoardType.NOTICE,
                    config = BoardConfig(writePermission = Role.ADMIN),
                )

            board.isAdminOnly shouldBe true
        }

        "updateConfig는 config를 교체한다" {
            val board = Board(id = 3L, name = "일반", type = BoardType.GENERAL)
            val newConfig = BoardConfig(commentEnabled = false, isPrivate = true)

            board.updateConfig(newConfig)

            board.config shouldBe newConfig
        }

        "markDeleted와 restore는 삭제 상태를 토글한다" {
            val board = Board(id = 4L, name = "운영", type = BoardType.GENERAL)

            board.markDeleted()
            board.isDeleted shouldBe true

            board.restore()
            board.isDeleted shouldBe false
        }
    })
