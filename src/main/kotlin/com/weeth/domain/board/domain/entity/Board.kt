package com.weeth.domain.board.domain.entity

import com.weeth.domain.board.domain.converter.BoardConfigConverter
import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.vo.BoardConfig
import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "board")
class Board(
    club: Club,
    name: String,
    type: BoardType,
    config: BoardConfig = BoardConfig(),
) : BaseEntity() {
    init {
        require(name.isNotBlank()) { "게시판 이름은 공백이 될 수 없습니다" }
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    var club: Club = club
        private set

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
        private set

    @Column(nullable = false)
    var name: String = name
        private set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: BoardType = type
        private set

    @Column(columnDefinition = "JSON") // Json 속성 사용으로 인한 커스텀 컨버터 적용
    @Convert(converter = BoardConfigConverter::class)
    var config: BoardConfig = config
        private set

    @Column(nullable = false)
    var isDeleted: Boolean = false
        private set

    val isCommentEnabled: Boolean
        get() = config.commentEnabled

    val isAdminOnly: Boolean
        get() = config.writePermission.isAdminOrLead()

    fun isAccessibleBy(memberRole: MemberRole): Boolean = memberRole.isAdminOrLead() || !config.isPrivate

    fun canWriteBy(memberRole: MemberRole): Boolean =
        isAccessibleBy(memberRole) && (memberRole.isAdminOrLead() || !isAdminOnly)

    fun updateConfig(newConfig: BoardConfig) {
        config = newConfig
    }

    fun rename(newName: String) {
        require(newName.isNotBlank()) { "게시판 이름은 공백이 될 수 없습니다." }
        name = newName
    }

    fun markDeleted() {
        isDeleted = true
    }

    fun restore() {
        isDeleted = false
    }
}
