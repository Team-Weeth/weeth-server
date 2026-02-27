package com.weeth.domain.user.domain.repository

import com.weeth.domain.user.application.exception.UserNotFoundException
import com.weeth.domain.user.domain.entity.Cardinal
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.enums.Status
import com.weeth.domain.user.domain.vo.Email
import com.weeth.domain.user.domain.vo.PhoneNumber
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import java.util.Optional

interface UserRepository :
    JpaRepository<User, Long>,
    UserReader {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("SELECT u FROM User u WHERE u.id = :id")
    fun findByIdWithLock(
        @Param("id") id: Long,
    ): Optional<User>

    fun findByEmail(email: Email): Optional<User>

    fun findAllByNameContainingAndStatus(
        name: String,
        status: Status,
    ): List<User>

    fun existsByEmail(email: Email): Boolean

    fun existsByStudentId(studentId: String): Boolean

    fun existsByTel(tel: PhoneNumber): Boolean

    fun existsByStudentIdAndIdIsNot(
        studentId: String,
        id: Long,
    ): Boolean

    fun existsByTelAndIdIsNot(
        tel: PhoneNumber,
        id: Long,
    ): Boolean

    fun findAllByStatusOrderByName(status: Status): List<User>

    fun findAllByOrderByNameAsc(): List<User>

    @Query("SELECT uc.user FROM UserCardinal uc WHERE uc.cardinal = :cardinal AND uc.user.status = :status")
    override fun findAllByCardinalAndStatus(
        @Param("cardinal") cardinal: Cardinal,
        @Param("status") status: Status,
    ): List<User>

    @Query(
        """
            SELECT u
            FROM User u
            JOIN UserCardinal uc ON u.id = uc.user.id
            JOIN uc.cardinal c
            WHERE u.status = :status
            GROUP BY u.id
            ORDER BY MAX(c.cardinalNumber) DESC, u.name ASC
        """,
    )
    fun findAllByStatusOrderedByCardinalAndName(
        @Param("status") status: Status,
        pageable: Pageable,
    ): Slice<User>

    @Query(
        """
            SELECT u FROM User u
            JOIN UserCardinal uc ON uc.user.id = u.id
            WHERE u.status = :status
            AND uc.cardinal = :cardinal
            ORDER BY u.name ASC
        """,
    )
    fun findAllByCardinalOrderByNameAsc(
        @Param("status") status: Status,
        @Param("cardinal") cardinal: Cardinal,
        pageable: Pageable,
    ): Slice<User>

    fun findByEmailValue(email: String): Optional<User> = findByEmail(Email.from(email))

    fun existsByEmailValue(email: String): Boolean = existsByEmail(Email.from(email))

    fun existsByTelValue(tel: String): Boolean = existsByTel(PhoneNumber.from(tel))

    fun existsByTelAndIdIsNotValue(
        tel: String,
        id: Long,
    ): Boolean = existsByTelAndIdIsNot(PhoneNumber.from(tel), id)

    override fun getById(userId: Long): User = findById(userId).orElseThrow { UserNotFoundException() }

    override fun getByEmail(email: String): User = findByEmailValue(email).orElseThrow { UserNotFoundException() }

    override fun findByIdOrNull(userId: Long): User? = findById(userId).orElse(null)

    override fun findAllByIds(userIds: List<Long>): List<User> = findAllById(userIds)
}
