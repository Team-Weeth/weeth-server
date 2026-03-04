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

    fun findTopByUserOrderByCardinalCardinalNumberDesc(user: User): UserCardinal?

    @Query(
        """
            SELECT uc
              FROM UserCardinal uc
              JOIN FETCH uc.cardinal
             WHERE uc.user IN :users
          ORDER BY uc.user.id, uc.cardinal.cardinalNumber DESC
        """,
    )
    fun findAllByUsers(
        @Param("users") users: List<User>,
    ): List<UserCardinal>

    @Query(
        """
            SELECT uc
              FROM UserCardinal uc
              JOIN FETCH uc.user
              JOIN FETCH uc.cardinal
          ORDER BY uc.user.name ASC
        """,
    )
    fun findAllWithUserAndCardinal(): List<UserCardinal>

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

    override fun findTopByUserOrderByCardinalNumberDesc(user: User): UserCardinal? =
        findTopByUserOrderByCardinalCardinalNumberDesc(user)
}
