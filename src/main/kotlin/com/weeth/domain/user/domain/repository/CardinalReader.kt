package com.weeth.domain.user.domain.repository

import com.weeth.domain.user.domain.entity.Cardinal

interface CardinalReader {
    fun getByCardinalNumber(cardinalNumber: Int): Cardinal

    fun findByIdOrNull(cardinalId: Long): Cardinal?

    fun findAllByCardinalNumberDesc(): List<Cardinal>
}
