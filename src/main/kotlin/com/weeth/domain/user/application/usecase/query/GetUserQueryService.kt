package com.weeth.domain.user.application.usecase.query

import com.weeth.domain.user.application.dto.response.AdminUserResponse
import com.weeth.domain.user.application.dto.response.UserDetailsResponse
import com.weeth.domain.user.application.dto.response.UserInfoResponse
import com.weeth.domain.user.application.dto.response.UserProfileResponse
import com.weeth.domain.user.application.dto.response.UserSummaryResponse
import com.weeth.domain.user.application.mapper.UserMapper
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.UserCardinal
import com.weeth.domain.user.domain.entity.enums.Status
import com.weeth.domain.user.domain.entity.enums.StatusPriority
import com.weeth.domain.user.domain.entity.enums.UsersOrderBy
import com.weeth.domain.user.domain.repository.CardinalReader
import com.weeth.domain.user.domain.repository.UserCardinalReader
import com.weeth.domain.user.domain.repository.UserCardinalRepository
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.domain.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.LinkedHashMap

@Service
@Transactional(readOnly = true)
class GetUserQueryService(
    private val userRepository: UserRepository,
    private val userReader: UserReader,
    private val cardinalReader: CardinalReader,
    private val userCardinalRepository: UserCardinalRepository,
    private val userCardinalReader: UserCardinalReader,
    private val mapper: UserMapper,
) {
    fun existsByEmail(email: String): Boolean = userRepository.existsByEmailValue(email)

    fun findAllUser(
        pageNumber: Int,
        pageSize: Int,
        cardinal: Int?,
    ): Slice<UserSummaryResponse> {
        val pageable = PageRequest.of(pageNumber, pageSize)
        val users: Slice<User> =
            if (cardinal == null) {
                userRepository.findAllByStatusOrderedByCardinalAndName(Status.ACTIVE, pageable)
            } else {
                val inputCardinal = cardinalReader.getByCardinalNumber(cardinal)
                userRepository.findAllByCardinalOrderByNameAsc(Status.ACTIVE, inputCardinal, pageable)
            }

        val allUserCardinals = userCardinalReader.findAllByUsersOrderByCardinalDesc(users.content)
        val userCardinalMap = allUserCardinals.groupBy { it.user.id }
        return users.map { user ->
            val userCardinals = userCardinalMap[user.id] ?: emptyList()
            mapper.toUserSummaryResponse(user, userCardinals)
        }
    }

    fun searchUser(keyword: String): List<UserSummaryResponse> {
        val users = userRepository.findAllByNameContainingAndStatus(keyword, Status.ACTIVE)
        val allUserCardinals = userCardinalReader.findAllByUsersOrderByCardinalDesc(users)
        val userCardinalMap = allUserCardinals.groupBy { it.user.id }
        return users.map { user ->
            val userCardinals = userCardinalMap[user.id] ?: emptyList()
            mapper.toUserSummaryResponse(user, userCardinals)
        }
    }

    fun findUserDetails(userId: Long): UserDetailsResponse {
        val user = userReader.getById(userId)
        val userCardinals = userCardinalReader.findAllByUser(user)
        return mapper.toUserDetailsResponse(user, userCardinals)
    }

    fun findMyProfile(userId: Long): UserProfileResponse {
        val user = userReader.getById(userId)
        val userCardinals = userCardinalReader.findAllByUser(user)
        return mapper.toUserProfileResponse(user, userCardinals)
    }

    fun findMyInfo(userId: Long): UserInfoResponse {
        val user = userReader.getById(userId)
        val userCardinals = userCardinalReader.findAllByUser(user)
        return mapper.toUserInfoResponse(user, userCardinals)
    }

    fun findAllByAdmin(orderBy: UsersOrderBy): List<AdminUserResponse> {
        val userCardinalMap: LinkedHashMap<User, List<UserCardinal>> =
            LinkedHashMap(
                userCardinalRepository.findAllByOrderByUserNameAsc().groupBy { it.user },
            )

        return when (orderBy) {
            UsersOrderBy.NAME_ASCENDING -> {
                userCardinalMap.entries
                    .sortedBy { StatusPriority.fromStatus(it.key.status).priority }
                    .map { entry ->
                        mapper.toAdminUserResponse(entry.key, entry.value)
                    }
            }

            UsersOrderBy.CARDINAL_DESCENDING -> {
                userCardinalMap.entries
                    .sortedWith(
                        compareBy<Map.Entry<User, List<UserCardinal>>> { StatusPriority.fromStatus(it.key.status).priority }
                            .thenByDescending { entry ->
                                entry.value.maxOfOrNull { it.cardinal.cardinalNumber } ?: -1
                            },
                    ).map { entry ->
                        mapper.toAdminUserResponse(entry.key, entry.value)
                    }
            }
        }
    }
}
