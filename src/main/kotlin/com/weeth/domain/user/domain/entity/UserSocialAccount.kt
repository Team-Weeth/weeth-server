package com.weeth.domain.user.domain.entity

import com.weeth.domain.user.domain.entity.enums.SocialProvider
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
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "user_social_account",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_provider_provider_user_id",
            columnNames = ["provider", "provider_user_id"],
        ),
    ],
)
class UserSocialAccount(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_social_account_id")
    val id: Long = 0L,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val provider: SocialProvider,
    @Column(name = "provider_user_id", nullable = false)
    val providerUserId: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
) : BaseEntity()
