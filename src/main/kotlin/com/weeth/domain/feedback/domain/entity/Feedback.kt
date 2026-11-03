package com.weeth.domain.feedback.domain.entity

import com.weeth.domain.feedback.domain.enums.FeedbackChannel
import com.weeth.domain.user.domain.entity.User
import com.weeth.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/**
 * 서비스 피드백. 이미지 등 첨부 확장 지점: 추후 `FileOwnerType.FEEDBACK`을 추가하고
 * 이 엔티티의 [id]를 ownerId로 연결하면 된다(이번 범위에서는 추가하지 않음).
 */
@Entity
@Table(
    name = "feedback",
    indexes = [
        Index(
            name = "idx_feedback_club_channel_created",
            columnList = "club_id, channel, created_at",
        ),
    ],
)
class Feedback private constructor(
    user: User,
    clubId: Long,
    channel: FeedbackChannel,
    content: String,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    var id: Long = 0L
        private set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User = user
        private set

    @Column(name = "club_id", nullable = false)
    var clubId: Long = clubId
        private set

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    var channel: FeedbackChannel = channel
        private set

    @Column(nullable = false, length = MAX_CONTENT_LENGTH)
    var content: String = content
        private set

    companion object {
        const val MAX_CONTENT_LENGTH = 1000

        /**
         * 피드백 내용이 유효한지 판단한다. 앞뒤 공백 제거 후 공백뿐이거나(유니코드 공백 포함,
         * Kotlin `isBlank()` 기준) [MAX_CONTENT_LENGTH]를 초과하면 유효하지 않다.
         *
         * `@NotBlank`는 U+3000(전각 공백) 등 일부 유니코드 공백을 공백으로 인식하지 못해
         * 요청 DTO 검증을 통과할 수 있으므로, UseCase가 저장 전에 이 메서드로 최종 확인한다.
         */
        fun isValidContent(content: String): Boolean {
            val normalized = content.trim()
            return normalized.isNotBlank() && normalized.length <= MAX_CONTENT_LENGTH
        }

        fun create(
            user: User,
            clubId: Long,
            channel: FeedbackChannel,
            content: String,
        ): Feedback {
            require(isValidContent(content)) { "피드백 내용이 올바르지 않습니다." }
            return Feedback(
                user = user,
                clubId = clubId,
                channel = channel,
                content = content.trim(),
            )
        }
    }
}
