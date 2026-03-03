package com.weeth.domain.board.domain.entity

import com.weeth.domain.board.domain.converter.BoardConfigConverter
import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.vo.BoardConfig
import com.weeth.domain.user.domain.enums.Role
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
    name: String,
    type: BoardType,
    config: BoardConfig = BoardConfig(),
    isDeleted: Boolean = false,
) : BaseEntity() {
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
    var isDeleted: Boolean = isDeleted
        private set

    val isCommentEnabled: Boolean
        get() = config.commentEnabled

    val isAdminOnly: Boolean
        get() = config.writePermission == Role.ADMIN

    fun isAccessibleBy(role: Role): Boolean = role == Role.ADMIN || !config.isPrivate

    fun canWriteBy(role: Role): Boolean = isAccessibleBy(role) && (!isAdminOnly || role == Role.ADMIN)

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
