package com.weeth.domain.board.domain.entity

import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.vo.BoardConfig
import com.weeth.domain.board.fixture.BoardTestFixture
import com.weeth.domain.club.domain.enums.MemberRole
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class BoardEntityTest :
    StringSpec({
        "isCommentEnabled는 config 값을 반영한다" {
            val board =
                BoardTestFixture.create(
                    name = "공지사항",
                    type = BoardType.NOTICE,
                    config = BoardConfig(commentEnabled = false),
                )

            board.isCommentEnabled shouldBe false
        }

        "rename은 빈 이름이면 예외를 던진다" {
            val board = BoardTestFixture.create(name = "게시판", type = BoardType.GENERAL)

            shouldThrow<IllegalArgumentException> {
                board.rename(" ")
            }
        }

        "isAdminOnly는 writePermission이 ADMIN일 때 true를 반환한다" {
            val board =
                BoardTestFixture.create(
                    name = "공지",
                    type = BoardType.NOTICE,
                    config = BoardConfig(writePermission = MemberRole.ADMIN),
                )

            board.isAdminOnly shouldBe true
        }

        "isAccessibleBy는 비공개 게시판을 ADMIN/LEAD에게만 허용한다" {
            val privateBoard =
                BoardTestFixture.create(
                    name = "운영",
                    type = BoardType.NOTICE,
                    config = BoardConfig(isPrivate = true),
                )

            privateBoard.isAccessibleBy(MemberRole.ADMIN) shouldBe true
            privateBoard.isAccessibleBy(MemberRole.LEAD) shouldBe true
            privateBoard.isAccessibleBy(MemberRole.USER) shouldBe false
        }

        "canWriteBy는 비공개/관리자 전용 설정을 모두 고려한다" {
            val privateBoard =
                BoardTestFixture.create(name = "비공개", type = BoardType.GENERAL, config = BoardConfig(isPrivate = true))
            val adminOnlyBoard =
                BoardTestFixture.create(
                    name = "공지",
                    type = BoardType.NOTICE,
                    config = BoardConfig(writePermission = MemberRole.ADMIN),
                )
            val publicBoard = BoardTestFixture.create(name = "일반", type = BoardType.GENERAL, config = BoardConfig())

            privateBoard.canWriteBy(MemberRole.USER) shouldBe false
            privateBoard.canWriteBy(MemberRole.ADMIN) shouldBe true
            privateBoard.canWriteBy(MemberRole.LEAD) shouldBe true
            adminOnlyBoard.canWriteBy(MemberRole.USER) shouldBe false
            adminOnlyBoard.canWriteBy(MemberRole.ADMIN) shouldBe true
            publicBoard.canWriteBy(MemberRole.USER) shouldBe true
        }

        "updateConfig는 config를 교체한다" {
            val board = BoardTestFixture.create(name = "일반", type = BoardType.GENERAL)
            val newConfig = BoardConfig(commentEnabled = false, isPrivate = true)

            board.updateConfig(newConfig)

            board.config shouldBe newConfig
        }

        "markDeleted와 restore는 삭제 상태를 토글한다" {
            val board = BoardTestFixture.create(name = "운영", type = BoardType.GENERAL)

            board.markDeleted()
            board.isDeleted shouldBe true

            board.restore()
            board.isDeleted shouldBe false
        }
    })
