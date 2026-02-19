package com.weeth.domain.board.domain.entity

import com.weeth.domain.board.domain.converter.BoardConfigConverter
import com.weeth.domain.board.domain.entity.enums.BoardType
import com.weeth.domain.board.domain.vo.BoardConfig
import com.weeth.domain.user.domain.entity.enums.Role
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "board")
class Board(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(nullable = false)
    var name: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: BoardType,
    @Column(columnDefinition = "JSON") // Json 속성 사용으로 인한 커스텀 컨버터 적용
    @Convert(converter = BoardConfigConverter::class)
    var config: BoardConfig = BoardConfig(),
    @Column(nullable = false)
    var isDeleted: Boolean = false,
) : BaseEntity() {
    val isCommentEnabled: Boolean
        get() = config.commentEnabled

    val isAdminOnly: Boolean
        get() = config.writePermission == Role.ADMIN

    fun isAccessibleBy(role: Role): Boolean = role == Role.ADMIN || !config.isPrivate

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
