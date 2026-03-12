package com.weeth.domain.dashboard.application.usecase.query

import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.repository.PostReader
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.dashboard.application.dto.response.DashboardHomeResponse
import com.weeth.domain.dashboard.application.dto.response.DashboardNoticeResponse
import com.weeth.domain.dashboard.application.dto.response.DashboardPostResponse
import com.weeth.domain.dashboard.application.dto.response.DashboardScheduleResponse
import com.weeth.domain.dashboard.application.dto.response.DashboardUnreadNoticeResponse
import com.weeth.domain.dashboard.application.exception.DashboardNotClubMemberException
import com.weeth.domain.dashboard.application.mapper.DashboardMapper
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.repository.FileReader
import com.weeth.domain.schedule.domain.repository.EventReader
import com.weeth.domain.session.domain.repository.SessionReader
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

// TODO: 해당 club 멤버에 해당하는지 검증 후 club의 DB만 조회
@Service
@Transactional(readOnly = true)
class GetDashboardQueryService(
    private val clubReader: ClubReader,
    private val clubMemberReader: ClubMemberReader,
    private val eventReader: EventReader,
    private val sessionReader: SessionReader,
    private val postReader: PostReader,
    private val fileReader: FileReader,
    private val dashboardMapper: DashboardMapper,
) {
    fun getHome(
        clubId: Long,
        userId: Long,
    ): DashboardHomeResponse {
        validateMembership(clubId, userId)

        val club = clubReader.getClubById(clubId)
        val memberCount = clubMemberReader.countActiveByClubId(clubId)

        // TODO: 해당 클럽 회원인지 검증 후 클럽의 오늘 일정만 조회
        val todayStart = LocalDate.now().atStartOfDay()
        val todayEnd = todayStart.plusDays(1).minusNanos(1)
        val todayEvents = eventReader.findByDateRange(todayStart, todayEnd)
        val todaySessions =
            sessionReader.findByStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(
                todayEnd,
                todayStart,
            )

        val myClubs = clubMemberReader.findActiveByUserId(userId).map(dashboardMapper::toMyClubResponse)

        return dashboardMapper.toHomeResponse(
            club = club,
            memberCount = memberCount,
            todaySchedules = dashboardMapper.toScheduleResponses(todayEvents, todaySessions),
            myClubs = myClubs,
        )
    }

    fun getRecentPosts(
        clubId: Long,
        userId: Long,
        pageNumber: Int,
        pageSize: Int,
    ): Slice<DashboardPostResponse> {
        validateMembership(clubId, userId)

        // TODO: 해당 클럽 회원인지 검증 후 클럽의 게시물만 조회
        val posts = postReader.findRecentExcludingBoardType(BoardType.NOTICE, PageRequest.of(pageNumber, pageSize))
        val now = LocalDateTime.now()
        val postIds = posts.content.map { it.id }
        val filesByPostId = fileReader.findAll(FileOwnerType.POST, postIds).groupBy { it.ownerId }

        return posts.map { post ->
            dashboardMapper.toPostResponse(
                post = post,
                authorProfileImage = null, // TODO: 유저 프로필 이미지 기능 구현 후 연동
                files = filesByPostId[post.id] ?: emptyList(),
                now = now,
            )
        }
    }

    fun getRecentNotices(
        clubId: Long,
        userId: Long,
    ): List<DashboardNoticeResponse> {
        validateMembership(clubId, userId)

        // TODO: 해당 클럽 회원인지 검증 후 클럽의 공지만 조회
        val notices = postReader.findRecentByBoardType(BoardType.NOTICE, PageRequest.of(0, RECENT_NOTICES_LIMIT))
        val now = LocalDateTime.now()

        return notices.content.map { dashboardMapper.toNoticeResponse(it, now) }
    }

    fun getMonthlySchedules(
        clubId: Long,
        userId: Long,
    ): List<DashboardScheduleResponse> {
        validateMembership(clubId, userId)

        // TODO: 해당 클럽 회원인지 검증 후 클럽의 일정만 조회
        val monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay()
        val monthEnd = monthStart.plusMonths(1).minusNanos(1)

        val events = eventReader.findByDateRange(monthStart, monthEnd)
        val sessions = sessionReader.findByStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(monthEnd, monthStart)

        return dashboardMapper.toScheduleResponses(events, sessions)
    }

    fun getUnreadNotice(
        clubId: Long,
        userId: Long,
    ): DashboardUnreadNoticeResponse? {
        validateMembership(clubId, userId)

        // TODO: 해당 클럽 회원인지 검증 후 클럽의 공지만 조회
        val since = LocalDateTime.now().minusWeeks(2)
        return postReader
            .findFirstUnreadNoticeSince(userId, BoardType.NOTICE, since)
            ?.let(dashboardMapper::toUnreadNoticeResponse)
    }

    companion object {
        private const val RECENT_NOTICES_LIMIT = 5
    }

    private fun validateMembership(
        clubId: Long,
        userId: Long,
    ) {
        val member =
            clubMemberReader.findByClubIdAndUserId(clubId, userId)
                ?: throw DashboardNotClubMemberException()
        if (!member.isActive()) throw DashboardNotClubMemberException()
    }
}
