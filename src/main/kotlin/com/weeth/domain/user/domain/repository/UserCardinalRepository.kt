package com.weeth.domain.user.domain.repository

import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.UserCardinal
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserCardinalRepository :
    JpaRepository<UserCardinal, Long>,
    UserCardinalReader {
    fun findAllByUserOrderByCardinalCardinalNumberDesc(user: User): List<UserCardinal>

    @Query("SELECT uc FROM UserCardinal uc WHERE uc.user IN :users ORDER BY uc.user.id, uc.cardinal.cardinalNumber DESC")
    fun findAllByUsers(
        @Param("users") users: List<User>,
    ): List<UserCardinal>

    fun findAllByOrderByUserNameAsc(): List<UserCardinal>

    @Query(
        """
            select uc.cardinal.cardinalNumber
              from UserCardinal uc
             where uc.user = :user
          order by uc.cardinal.cardinalNumber desc
        """,
    )
    fun findCardinalNumbersByUser(
        @Param("user") user: User,
    ): List<Int>

    override fun findAllByUser(user: User): List<UserCardinal> = findAllByUserOrderByCardinalCardinalNumberDesc(user)

    override fun findAllByUsersOrderByCardinalDesc(users: List<User>): List<UserCardinal> = findAllByUsers(users)

    override fun getCardinalNumbers(user: User): List<Int> = findCardinalNumbersByUser(user)
}
