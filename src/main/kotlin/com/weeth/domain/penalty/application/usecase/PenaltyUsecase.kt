package com.weeth.domain.penalty.application.usecase

import com.weeth.domain.penalty.application.dto.PenaltyDTO

interface PenaltyUsecase {
    fun save(dto: PenaltyDTO.Save)

    fun update(dto: PenaltyDTO.Update)

    fun findAll(cardinalNumber: Int?): List<PenaltyDTO.ResponseAll>

    fun find(userId: Long): PenaltyDTO.Response

    fun delete(penaltyId: Long)
}
