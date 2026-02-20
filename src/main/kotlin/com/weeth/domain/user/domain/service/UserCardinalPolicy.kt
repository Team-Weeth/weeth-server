package com.weeth.domain.user.domain.service

import com.weeth.domain.user.application.exception.CardinalNotFoundException
import com.weeth.domain.user.domain.entity.Cardinal
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.repository.UserCardinalReader
import org.springframework.stereotype.Service

@Service
class UserCardinalPolicy(
    private val userCardinalReader: UserCardinalReader,
) {
    fun getCurrentCardinal(user: User): Cardinal =
        userCardinalReader
            .findAllByUser(user)
            .maxByOrNull { it.cardinal.cardinalNumber }
            ?.cardinal
            ?: throw CardinalNotFoundException()

    fun notContains(
        user: User,
        cardinal: Cardinal,
    ): Boolean = userCardinalReader.findAllByUser(user).none { it.cardinal.id == cardinal.id }

    fun isCurrent(
        user: User,
        cardinal: Cardinal,
    ): Boolean = getCurrentCardinal(user).cardinalNumber < cardinal.cardinalNumber
}
