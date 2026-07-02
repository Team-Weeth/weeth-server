package com.weeth.domain.user.domain.entity

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

@Entity
@Table(name = "user_profile")
class UserProfile(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    name: String,
    profileImageStorageKey: String? = null,
    headerImageStorageKey: String? = null,
    bio: String? = null,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_profile_id")
    var id: Long = 0L
        private set

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    var name: String = normalizeName(name)
        private set

    @Column(name = "profile_image_storage_key", length = MAX_STORAGE_KEY_LENGTH)
    var profileImageStorageKey: String? = normalizeStorageKey(profileImageStorageKey, "프로필 이미지")
        private set

    @Column(name = "header_image_storage_key", length = MAX_STORAGE_KEY_LENGTH)
    var headerImageStorageKey: String? = normalizeStorageKey(headerImageStorageKey, "헤더 이미지")
        private set

    @Column(length = MAX_BIO_LENGTH)
    var bio: String? = normalizeBio(bio)
        private set

    fun update(
        name: String? = null,
        profileImageStorageKey: String? = null,
        headerImageStorageKey: String? = null,
        bio: String? = null,
    ) {
        name?.let { this.name = normalizeName(it) }
        profileImageStorageKey?.let {
            this.profileImageStorageKey = normalizeStorageKey(it, "프로필 이미지")
        }
        headerImageStorageKey?.let {
            this.headerImageStorageKey = normalizeStorageKey(it, "헤더 이미지")
        }
        bio?.let { this.bio = normalizeBio(it) }
    }

    fun removeProfileImage() {
        profileImageStorageKey = null
    }

    fun removeHeaderImage() {
        headerImageStorageKey = null
    }

    fun isOwnedBy(userId: Long): Boolean = user.id == userId

    companion object {
        private const val MAX_NAME_LENGTH = 20
        private const val MAX_STORAGE_KEY_LENGTH = 500
        private const val MAX_BIO_LENGTH = 30

        fun create(
            user: User,
            name: String,
            profileImageStorageKey: String? = null,
            headerImageStorageKey: String? = null,
            bio: String? = null,
        ): UserProfile =
            UserProfile(
                user = user,
                name = name,
                profileImageStorageKey = profileImageStorageKey,
                headerImageStorageKey = headerImageStorageKey,
                bio = bio,
            )

        private fun normalizeName(name: String): String {
            val trimmed = name.trim()
            require(trimmed.isNotBlank()) { "프로필 이름은 공백일 수 없습니다." }
            require(trimmed.length <= MAX_NAME_LENGTH) { "프로필 이름은 ${MAX_NAME_LENGTH}자 이하여야 합니다." }
            return trimmed
        }

        private fun normalizeStorageKey(
            storageKey: String?,
            label: String,
        ): String? {
            val trimmed = storageKey?.trim()?.takeIf { it.isNotBlank() }
            require((trimmed?.length ?: 0) <= MAX_STORAGE_KEY_LENGTH) {
                "$label storageKey는 ${MAX_STORAGE_KEY_LENGTH}자 이하여야 합니다."
            }
            return trimmed
        }

        private fun normalizeBio(bio: String?): String? {
            val trimmed = bio?.trim()?.takeIf { it.isNotBlank() }
            require((trimmed?.length ?: 0) <= MAX_BIO_LENGTH) { "자기소개는 ${MAX_BIO_LENGTH}자 이하여야 합니다." }
            return trimmed
        }
    }
}
