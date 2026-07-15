package com.weeth.domain.user.application.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive

data class AssignClubProfileRequest(
    @field:Schema(description = "동아리별 프로필 설정 목록")
    @field:NotEmpty
    @field:Valid
    val assignments: List<ClubProfileAssignmentRequest>,
)

data class ClubProfileAssignmentRequest(
    @field:Schema(description = "동아리 ID", example = "1A2b3C")
    @field:NotBlank
    val clubId: String,
    @field:Schema(description = "사용할 프로필 ID", example = "1")
    @field:Positive
    val profileId: Long,
)
