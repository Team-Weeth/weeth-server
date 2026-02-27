package com.weeth.domain.user.domain.repository

import com.weeth.domain.user.domain.entity.Cardinal
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.enums.Status

interface UserReader {
    fun getById(userId: Long): User

    fun getByEmail(email: String): User

    fun findByIdOrNull(userId: Long): User?

    fun findAllByIds(userIds: List<Long>): List<User>

    fun findAllByCardinalAndStatus(
        cardinal: Cardinal,
        status: Status,
    ): List<User>
}
