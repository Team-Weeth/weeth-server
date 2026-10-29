package com.weeth.domain.feedback.domain.entity

import com.weeth.domain.feedback.domain.enums.FeedbackChannel
import com.weeth.domain.feedback.domain.exception.InvalidFeedbackContentException
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

        fun create(
            user: User,
            clubId: Long,
            channel: FeedbackChannel,
            content: String,
        ): Feedback {
            val normalized = content.trim()
            // `@NotBlank`는 U+3000(전각 공백) 등 일부 유니코드 공백을 공백으로 인식하지 못해,
            // 요청 DTO 검증을 통과하더라도 여기서 최종적으로 걸러내야 한다. 검증 실패는
            // IllegalArgumentException이 아닌 도메인 예외로 던져 CommonExceptionHandler가 400으로 매핑하게 한다.
            if (normalized.isBlank() || normalized.length > MAX_CONTENT_LENGTH) {
                throw InvalidFeedbackContentException()
            }
            return Feedback(
                user = user,
                clubId = clubId,
                channel = channel,
                content = normalized,
            )
        }
    }
}
