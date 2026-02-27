package com.weeth.domain.user.domain.repository

import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.UserCardinal

interface UserCardinalReader {
    fun findAllByUser(user: User): List<UserCardinal>

    fun findAllByUsersOrderByCardinalDesc(users: List<User>): List<UserCardinal>

    fun findTopByUserOrderByCardinalNumberDesc(user: User): UserCardinal?
}
