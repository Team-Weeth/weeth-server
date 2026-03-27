package com.weeth.domain.session.application.usecase.query

import com.weeth.domain.cardinal.application.exception.CardinalNotFoundException
import com.weeth.domain.cardinal.domain.repository.CardinalReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import com.weeth.domain.session.application.dto.response.SessionResponse
import com.weeth.domain.session.application.dto.response.SessionGroupResponse
import com.weeth.domain.session.application.dto.response.SessionInfosResponse
import com.weeth.domain.session.application.exception.SessionNotFoundException
import com.weeth.domain.session.application.mapper.SessionMapper
import com.weeth.domain.session.domain.entity.Session
import com.weeth.domain.session.domain.repository.SessionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Service
@Transactional(readOnly = true)
class GetSessionQueryService(
    private val sessionRepository: SessionRepository,
    private val cardinalReader: CardinalReader,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val clubPermissionPolicy: ClubPermissionPolicy,
    private val sessionMapper: SessionMapper,
) {
    fun findSession(
        clubId: Long,
        userId: Long,
        sessionId: Long,
    ): SessionResponse {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)
        val session = sessionRepository.findByIdAndClubId(sessionId, clubId) ?: throw SessionNotFoundException()

        return if (member.isAdminOrLead()) {
            sessionMapper.toAdminResponse(session)
        } else {
            sessionMapper.toResponse(session)
        }
    }

    fun findSessionInfos(
        clubId: Long,
        userId: Long,
        cardinal: Int?,
    ): SessionInfosResponse {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        if (cardinal != null) {
            cardinalReader.findByClubIdAndCardinalNumber(clubId, cardinal)
                ?: throw CardinalNotFoundException()
        }
        val sessions =
            if (cardinal == null) {
                sessionRepository.findAllByClubIdOrderByStartDesc(clubId)
            } else {
                sessionRepository.findAllByClubIdAndCardinalOrderByStartDesc(clubId, cardinal)
            }

        val thisWeek = findThisWeek(sessions)
        val groupedResponses = buildGroupResponses(sessions)

        return sessionMapper.toInfos(thisWeek, groupedResponses)
    }

    private fun buildGroupResponses(sessions: List<Session>): List<SessionGroupResponse> {
        // 반복 세션은 그룹별로 묶고, 비반복 세션은 개별로 처리
        val groupResponses =
            sessions
                .filter { it.isRecurring }
                .groupBy { checkNotNull(it.sessionGroup).id }
                .map { (_, groupSessions) ->
                    val group = checkNotNull(groupSessions.first().sessionGroup)
                    sessionMapper.toGroupResponse(group, groupSessions)
                }

        val singleResponses =
            sessions
                .filter { !it.isRecurring }
                .map { sessionMapper.toSingleGroupResponse(it) }

        // 시작일 기준 내림차순 정렬
        return (groupResponses + singleResponses).sortedByDescending { it.startDate }
    }

    private fun findThisWeek(sessions: List<Session>): List<Session> {
        val today = LocalDate.now()
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

        return sessions.filter { s ->
            val d = s.start.toLocalDate()
            !d.isBefore(startOfWeek) && !d.isAfter(endOfWeek)
        }
    }
}
