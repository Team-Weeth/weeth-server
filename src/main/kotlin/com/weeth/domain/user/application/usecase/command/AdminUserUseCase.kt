package com.weeth.domain.user.application.usecase.command

import com.weeth.domain.attendance.domain.entity.Attendance
import com.weeth.domain.attendance.domain.repository.AttendanceRepository
import com.weeth.domain.cardinal.domain.entity.Cardinal
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.session.domain.repository.SessionReader
import com.weeth.domain.user.application.dto.request.UserApplyObRequest
import com.weeth.domain.user.application.dto.request.UserIdsRequest
import com.weeth.domain.user.application.dto.request.UserRoleUpdateRequest
import com.weeth.domain.user.domain.entity.User
import com.weeth.domain.user.domain.entity.UserCardinal
import com.weeth.domain.user.domain.repository.UserCardinalRepository
import com.weeth.domain.user.domain.repository.UserReader
import com.weeth.domain.user.domain.service.UserCardinalPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminUserUseCase(
    private val userReader: UserReader,
    private val userCardinalPolicy: UserCardinalPolicy,
    private val cardinalReader: CardinalReader,
    private val sessionReader: SessionReader,
    private val attendanceRepository: AttendanceRepository,
    private val userCardinalRepository: UserCardinalRepository,
) {
    @Transactional
    fun accept(request: UserIdsRequest) {
        val users = userReader.findAllByIds(request.userId)
        users.forEach { user ->
            val cardinal = userCardinalPolicy.getCurrentCardinal(user)

            if (user.isInactive()) {
                user.accept()
                initializeAttendances(listOf(user), cardinal)
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

    /**
     * 이전 기수의 인원들을 다음 기수로 한 번에 등록하는 메서드.
     * N+1을 해소하는 비용이 코드 가독성에 비해 지나치게 커서 배치 조회 + 캐싱 방식으로 절충하였다.
     */
    @Transactional
    fun applyOb(requests: List<UserApplyObRequest>) {
        // 동일한 (userId, cardinal) 요청은 한 번만 처리한다.
        val uniqueRequests = requests.distinctBy { it.userId to it.cardinal }
        if (uniqueRequests.isEmpty()) return

        // 유저는 한 번에 조회해 요청 수만큼 getById가 반복되는 것을 줄인다.
        val usersById =
            userReader
                .findAllByIds(uniqueRequests.map { it.userId }.distinct())
                .associateBy { it.id }

        // 같은 기수 번호 조회가 반복되지 않도록 메모리 캐시를 사용한다.
        val cardinalByNumber = mutableMapOf<Int, Cardinal>()

        uniqueRequests.forEach { req ->
            // 배치 조회에서 누락된 id는 기존과 동일하게 getById로 예외를 발생시킨다.
            val user = usersById[req.userId] ?: userReader.getById(req.userId)
            val nextCardinal =
                cardinalByNumber.getOrPut(req.cardinal) {
                    cardinalReader.getByCardinalNumber(req.cardinal)
                }

            if (userCardinalPolicy.notContains(user, nextCardinal)) {
                if (userCardinalPolicy.isCurrent(user, nextCardinal)) {
                    user.resetAttendanceStats()
                    initializeAttendances(listOf(user), nextCardinal)
                }

                userCardinalRepository.save(UserCardinal.create(user, nextCardinal))
            }
        }
    }

    private fun initializeAttendances(
        users: List<User>,
        cardinal: Cardinal,
    ) {
        if (users.isEmpty()) return
        val sessions = sessionReader.findAllByCardinal(cardinal.cardinalNumber)
        if (sessions.isEmpty()) return

        attendanceRepository.saveAll(
            users.flatMap { user ->
                sessions.map { Attendance.create(it, user) }
            },
        )
    }
}
