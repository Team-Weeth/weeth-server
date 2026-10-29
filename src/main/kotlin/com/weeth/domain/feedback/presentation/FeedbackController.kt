package com.weeth.domain.feedback.presentation

import com.weeth.domain.club.application.exception.ClubErrorCode
import com.weeth.domain.feedback.application.dto.request.CreateFeedbackRequest
import com.weeth.domain.feedback.application.usecase.command.CreateFeedbackUseCase
import com.weeth.global.auth.annotation.CurrentUser
import com.weeth.global.auth.jwt.application.exception.JwtErrorCode
import com.weeth.global.common.exception.ApiErrorCodeExample
import com.weeth.global.common.response.CommonResponse
import com.weeth.global.common.web.TsidParam
import com.weeth.global.common.web.TsidPathVariable
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "FEEDBACK", description = "서비스 피드백 API")
@RestController
@RequestMapping("/api/v4/clubs/{clubId}/feedbacks")
@ApiErrorCodeExample(ClubErrorCode::class, JwtErrorCode::class)
class FeedbackController(
    private val createFeedbackUseCase: CreateFeedbackUseCase,
) {
    @PostMapping
    @Operation(summary = "서비스 피드백 저장")
    fun save(
        @TsidParam
        @TsidPathVariable clubId: Long,
        @RequestBody @Valid request: CreateFeedbackRequest,
        @Parameter(hidden = true) @CurrentUser userId: Long,
    ): CommonResponse<Void?> {
        createFeedbackUseCase.execute(clubId, request, userId)
        return CommonResponse.success(FeedbackResponseCode.FEEDBACK_CREATED_SUCCESS)
    }
}
