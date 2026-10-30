package com.weeth.domain.feedback.application.dto.request

import com.fasterxml.jackson.annotation.JsonFormat
import com.weeth.domain.feedback.domain.entity.Feedback
import com.weeth.domain.feedback.domain.enums.FeedbackChannel
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * 이미지 등 첨부 확장 지점: 추후 `files: List<FileSaveRequest>?` 필드를 추가하고
 * `FileOwnerType.FEEDBACK` + 생성된 피드백 id로 연결하면 된다(이번 범위에서는 추가하지 않음).
 */
data class CreateFeedbackRequest(
    @field:Schema(description = "피드백 내용", example = "출석 체크 화면이 느려요.")
    // content가 누락되면(null) Kotlin non-null 파라미터 역직렬화 예외(500) 대신 null을 받아
    // @NotBlank 검증으로 400 처리한다. @NotBlank는 null도 거부한다.
    @field:NotBlank
    @field:Size(max = Feedback.MAX_CONTENT_LENGTH)
    val content: String?,
    @field:Schema(description = "피드백 채널", example = "CHAT")
    // 허용되지 않은 문자열은 JSON 파싱 예외(500) 대신 null로 받아 @NotNull 검증으로 400 처리한다.
    @field:JsonFormat(with = [JsonFormat.Feature.READ_UNKNOWN_ENUM_VALUES_AS_NULL])
    @field:NotNull
    val channel: FeedbackChannel?,
)
