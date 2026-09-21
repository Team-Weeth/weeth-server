package com.weeth.domain.club.domain.entity

import com.weeth.domain.club.domain.enums.PositionColor
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
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

/**
 * 동아리 관리자가 설정하는 멤버 포지션(직책) 옵션.
 * 동아리당 최대 6개까지 설정 가능하며, displayOrder로 관리자가 지정한 표시 순서를 저장한다.
 */
@Entity
@Table(name = "club_position_option")
class ClubPositionOption(
    club: Club,
    id: Long = 0L,
    name: String,
    color: PositionColor,
    displayOrder: Int,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_position_option_id")
    var id: Long = id
        private set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    var club: Club = club
        private set

    @Column(nullable = false, length = 10)
    var name: String = name
        private set

    @Column(name = "color", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    var color: PositionColor = color
        private set

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = displayOrder
        private set

    fun update(
        name: String,
        color: PositionColor,
        displayOrder: Int,
    ) {
        require(name.isNotBlank()) { "포지션 이름은 비어 있을 수 없습니다." }
        require(name.length <= MAX_NAME_LENGTH) { "포지션 이름은 최대 ${MAX_NAME_LENGTH}자까지 입력할 수 있습니다." }
        this.name = name
        this.color = color
        this.displayOrder = displayOrder
    }

    companion object {
        const val MAX_NAME_LENGTH = 10
        const val MAX_OPTION_COUNT = 6

        fun create(
            club: Club,
            name: String,
            color: PositionColor,
            displayOrder: Int,
        ): ClubPositionOption {
            require(name.isNotBlank()) { "포지션 이름은 비어 있을 수 없습니다." }
            require(name.length <= MAX_NAME_LENGTH) { "포지션 이름은 최대 ${MAX_NAME_LENGTH}자까지 입력할 수 있습니다." }
            return ClubPositionOption(
                club = club,
                name = name,
                color = color,
                displayOrder = displayOrder,
            )
        }
    }
}
