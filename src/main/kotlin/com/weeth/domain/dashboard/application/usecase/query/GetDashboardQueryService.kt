package com.weeth.domain.dashboard.application.usecase.query

import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.repository.PostReader
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.dashboard.application.dto.response.DashboardHomeResponse
import com.weeth.domain.dashboard.application.dto.response.DashboardNoticeResponse
import com.weeth.domain.dashboard.application.dto.response.DashboardPostResponse
import com.weeth.domain.dashboard.application.dto.response.DashboardScheduleResponse
import com.weeth.domain.dashboard.application.dto.response.DashboardUnreadNoticeResponse
import com.weeth.domain.dashboard.application.mapper.DashboardMapper
import com.weeth.domain.user.domain.repository.UserReader
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

@Service
@Transactional(readOnly = true)
class GetDashboardQueryService(
    private val clubReader: ClubReader,
    private val clubMemberReader: ClubMemberReader,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val eventReader: EventReader,
    private val sessionReader: SessionReader,
    private val postReader: PostReader,
    private val fileReader: FileReader,
    private val userReader: UserReader,
    private val dashboardMapper: DashboardMapper,
) {
    fun getHome(
        clubId: Long,
        userId: Long,
    ): DashboardHomeResponse {
        clubMemberPolicy.getActiveMember(clubId, userId)

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
        val myInfo = dashboardMapper.toMyInfoResponse(userReader.getById(userId))

        return dashboardMapper.toHomeResponse(
            club = club,
            memberCount = memberCount,
            myInfo = myInfo,
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
        clubMemberPolicy.getActiveMember(clubId, userId)

        val posts = postReader.findRecentByClubIdExcludingBoardType(clubId, BoardType.NOTICE, PageRequest.of(pageNumber, pageSize))
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
        size: Int,
    ): List<DashboardNoticeResponse> {
        clubMemberPolicy.getActiveMember(clubId, userId)

        val notices = postReader.findRecentByClubIdAndBoardType(clubId, BoardType.NOTICE, PageRequest.of(0, size))
        val now = LocalDateTime.now()

        return notices.content.map { dashboardMapper.toNoticeResponse(it, now) }
    }

    fun getMonthlySchedules(
        clubId: Long,
        userId: Long,
    ): List<DashboardScheduleResponse> {
        clubMemberPolicy.getActiveMember(clubId, userId)

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
        clubMemberPolicy.getActiveMember(clubId, userId)

        val since = LocalDateTime.now().minusWeeks(2)
        return postReader
            .findFirstUnreadNoticeSince(clubId, userId, BoardType.NOTICE, since)
            ?.let(dashboardMapper::toUnreadNoticeResponse)
    }

}
