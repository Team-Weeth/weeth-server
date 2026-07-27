package com.weeth.domain.dashboard.application.mapper

import com.weeth.domain.board.application.dto.response.BoardConfigResponse
import com.weeth.domain.board.application.dto.response.PostLikeResponse
import com.weeth.domain.board.domain.entity.Post
import com.weeth.domain.club.domain.entity.Club
import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.dashboard.application.dto.response.DashboardClubInfoResponse
import com.weeth.domain.dashboard.application.dto.response.DashboardHomeResponse
import com.weeth.domain.dashboard.application.dto.response.DashboardMyClubResponse
import com.weeth.domain.dashboard.application.dto.response.DashboardMyInfoResponse
import com.weeth.domain.dashboard.application.dto.response.DashboardNoticeResponse
import com.weeth.domain.dashboard.application.dto.response.DashboardPostResponse
import com.weeth.domain.dashboard.application.dto.response.DashboardScheduleResponse
import com.weeth.domain.dashboard.application.dto.response.DashboardUnreadNoticeResponse
import com.weeth.domain.dashboard.domain.enums.ScheduleType
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.schedule.domain.entity.Event
import com.weeth.domain.session.domain.entity.Session
import com.weeth.domain.user.application.mapper.UserInfoMapper
import com.weeth.global.common.id.TsidBase62Encoder
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class DashboardMapper(
    private val fileMapper: FileMapper,
    private val fileAccessUrlPort: FileAccessUrlPort,
    private val userInfoMapper: UserInfoMapper,
) {
    fun toClubInfoResponse(
        club: Club,
        memberCount: Long,
    ) = DashboardClubInfoResponse(
        id = TsidBase62Encoder.encode(club.id),
        name = club.name,
        schoolName = club.schoolName,
        description = club.description,
        memberCount = memberCount,
        profileImageUrl = resolveClubImage(club.profileImageStorageKey),
        backgroundImageUrl = resolveClubImage(club.backgroundImageStorageKey),
        code = club.code,
    )

    fun toMyInfoResponse(clubMember: ClubMember) =
        DashboardMyInfoResponse(
            userInfo = userInfoMapper.toClubMemberAuthorInfo(clubMember),
            bio = clubMember.bio,
        )

    fun toHomeResponse(
        club: Club,
        memberCount: Long,
        myInfo: DashboardMyInfoResponse,
        accountVisible: Boolean,
        todaySchedules: List<DashboardScheduleResponse>,
        myClubs: List<DashboardMyClubResponse>,
    ) = DashboardHomeResponse(
        club = toClubInfoResponse(club, memberCount),
        myInfo = myInfo,
        accountVisible = accountVisible,
        todaySchedules = todaySchedules,
        myClubs = myClubs,
    )

    fun toMyClubResponse(cm: ClubMember) =
        DashboardMyClubResponse(
            id = TsidBase62Encoder.encode(cm.club.id),
            name = cm.club.name,
            schoolName = cm.club.schoolName,
            description = cm.club.description,
            profileImageUrl = resolveClubImage(cm.club.profileImageStorageKey),
        )

    fun toScheduleResponses(
        events: List<Event>,
        sessions: List<Session>,
    ): List<DashboardScheduleResponse> =
        (events.map(::toScheduleResponse) + sessions.map(::toScheduleResponse))
            .sortedBy { it.start }

    fun toScheduleResponse(event: Event) =
        DashboardScheduleResponse(
            id = event.id,
            title = event.title,
            start = event.start,
            end = event.end,
            type = ScheduleType.EVENT,
        )

    fun toScheduleResponse(session: Session) =
        DashboardScheduleResponse(
            id = session.id,
            title = session.title,
            start = session.start,
            end = session.end,
            type = ScheduleType.SESSION,
        )

    fun toPostResponse(
        post: Post,
        files: List<File>,
        now: LocalDateTime,
        isLiked: Boolean,
        memberRole: MemberRole,
    ) = DashboardPostResponse(
        id = post.id,
        boardId = post.board.id,
        author = userInfoMapper.toClubMemberAuthorInfo(post.clubMember),
        title = post.title,
        content = post.content,
        time = post.createdAt,
        commentCount = post.commentCount,
        like = PostLikeResponse(isLiked = isLiked, likeCount = post.likeCount),
        fileUrls = files.map(fileMapper::toFileResponse),
        isNew = post.createdAt.isAfter(now.minusHours(24)),
        boardConfig = BoardConfigResponse.of(post.board, memberRole),
    )

    fun toNoticeResponse(
        post: Post,
        now: LocalDateTime,
    ) = DashboardNoticeResponse(
        id = post.id,
        boardId = post.board.id,
        title = post.title,
        content = post.content,
        time = post.createdAt,
        isNew = post.createdAt.isAfter(now.minusHours(24)),
    )

    fun toUnreadNoticeResponse(post: Post) =
        DashboardUnreadNoticeResponse(
            id = post.id,
            boardId = post.board.id,
            title = post.title,
            content = post.content,
        )

    private fun resolveClubImage(storageKey: String?): String? = storageKey?.let { fileAccessUrlPort.resolve(it) }
}
