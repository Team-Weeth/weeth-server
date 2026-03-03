package com.weeth.domain.user.domain.entity

import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "user_cardinal",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_id_cardinal_id",
            columnNames = ["user_id", "cardinal_id"],
        ),
    ],
)
class UserCardinal(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cardinal_id", nullable = false)
    val cardinal: Cardinal,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_cardinal_id")
    val id: Long = 0L

    companion object {
        fun create(
            user: User,
            cardinal: Cardinal,
        ) = UserCardinal(
            user = user,
            cardinal = cardinal,
        )
    }
}
