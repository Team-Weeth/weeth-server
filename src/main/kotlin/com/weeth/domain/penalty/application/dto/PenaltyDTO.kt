package com.weeth.domain.penalty.application.dto

import com.weeth.domain.penalty.domain.entity.enums.PenaltyType
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

class PenaltyDTO {
    data class Save(
        @field:NotNull
        val userId: Long,
        @field:NotNull
        val penaltyType: PenaltyType,
        val penaltyDescription: String?,
    )

    data class Update(
        @field:NotNull
        val penaltyId: Long,
        val penaltyDescription: String?,
    )

    data class ResponseAll(
        val cardinal: Int?,
        val responses: List<Response>,
    )

    data class Response(
        val userId: Long?,
        val penaltyCount: Int?,
        val warningCount: Int?,
        val name: String?,
        val cardinals: List<Int>,
        val penalties: List<Penalties>,
    )

    data class Penalties(
        val penaltyId: Long?,
        val penaltyType: PenaltyType?,
        val cardinal: Int?,
        val penaltyDescription: String?,
        val time: LocalDateTime?,
    )
}
