package com.weeth.domain.user.domain.repository

import com.weeth.domain.user.domain.entity.User

interface UserReader {
    fun getById(userId: Long): User

    fun getByEmail(email: String): User

    fun findByIdOrNull(userId: Long): User?

    fun findAllByIds(userIds: List<Long>): List<User>
}
