package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.attendance.domain.repository.SessionReader
import com.weeth.domain.user.application.dto.request.UserApplyObRequest
import com.weeth.domain.user.application.dto.request.UserIdsRequest
import com.weeth.domain.user.application.dto.request.UserRoleUpdateRequest
import com.weeth.domain.user.application.exception.CardinalNotFoundException
import com.weeth.domain.user.domain.entity.Cardinal
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.UserCardinal
import com.weeth.domain.user.domain.repository.CardinalRepository
import com.weeth.domain.user.domain.repository.UserCardinalRepository
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.domain.service.UserCardinalPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminUserUseCase(
    private val userReader: UserReader,
    private val sessionReader: SessionReader,
    private val attendanceRepository: AttendanceRepository,
    private val cardinalRepository: CardinalRepository,
    private val userCardinalRepository: UserCardinalRepository,
    private val userCardinalPolicy: UserCardinalPolicy,
) {
    @Transactional
    fun accept(request: UserIdsRequest) {
        val users = userReader.findAllByIds(request.userId)
        users.forEach { user ->
            val cardinal = userCardinalPolicy.getCurrentCardinal(user).cardinalNumber
            if (user.isInactive()) {
                user.accept()
                val sessions = sessionReader.findAllByCardinal(cardinal)
                attendanceRepository.saveAll(sessions.map { Attendance.create(it, user) })
            }
        }
    }

    @Transactional
    fun updateRole(request: List<UserRoleUpdateRequest>) {
        request.forEach { req ->
            val user = userReader.getById(req.userId)
            user.updateRole(req.role)
        }
    }

    @Transactional
    fun ban(request: UserIdsRequest) {
        val users = userReader.findAllByIds(request.userId)
        users.forEach { user ->
            user.ban()
        }
    }

    @Transactional
    fun applyOb(requests: List<UserApplyObRequest>) { // todo: 리팩토링
        if (requests.isEmpty()) return

        val distinctUserIds = requests.map { it.userId }.distinct()
        val users = userReader.findAllByIds(distinctUserIds)
        val userMap = users.associateBy { it.id }
        distinctUserIds.firstOrNull { it !in userMap }?.let { userReader.getById(it) }

        val existingCardinalsByUser = userCardinalRepository.findAllByUsers(users).groupBy { it.user.id }
        val cardinalMap = getOrCreateCardinals(requests.map { it.cardinal }.distinct())

        val newLinks = mutableListOf<Pair<User, Cardinal>>()
        val initNeededByCardinal = mutableMapOf<Int, MutableList<User>>()

        requests.forEach { req ->
            val user = userMap.getValue(req.userId)
            val nextCardinal = cardinalMap.getValue(req.cardinal)
            val existing = existingCardinalsByUser[user.id] ?: emptyList()

            if (existing.any { it.cardinal.id == nextCardinal.id }) return@forEach

            val maxCardinalNumber =
                existing.maxOfOrNull { it.cardinal.cardinalNumber } ?: throw CardinalNotFoundException()

            if (maxCardinalNumber < nextCardinal.cardinalNumber) {
                user.resetAttendanceStats()
                initNeededByCardinal.getOrPut(req.cardinal) { mutableListOf() }.add(user)
            }
            newLinks.add(user to nextCardinal)
        }

        if (initNeededByCardinal.isNotEmpty()) {
            val sessionsByCardinal =
                sessionReader.findAllByCardinalIn(initNeededByCardinal.keys.toList()).groupBy { it.cardinal }
            initNeededByCardinal.forEach { (cardinalNumber, usersToInit) ->
                val sessions = sessionsByCardinal[cardinalNumber] ?: emptyList()
                usersToInit.forEach { user ->
                    attendanceRepository.saveAll(sessions.map { Attendance.create(it, user) })
                }
            }
        }

        newLinks.forEach { (user, cardinal) -> userCardinalRepository.save(UserCardinal(user, cardinal)) }
    }

    private fun getOrCreateCardinals(cardinalNumbers: List<Int>): Map<Int, Cardinal> {
        val existing = cardinalRepository.findAllByCardinalNumberIn(cardinalNumbers).associateBy { it.cardinalNumber }
        return cardinalNumbers.associateWith { num ->
            existing[num] ?: cardinalRepository.save(Cardinal.create(cardinalNumber = num))
        }
    }
}
