package com.weeth.domain.user.application.usecase.query

import com.weeth.domain.attendance.domain.enums.AttendanceStatus
import com.weeth.domain.attendance.domain.repository.AttendanceReader
import com.weeth.domain.board.domain.repository.PostReader
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.club.domain.repository.ClubMemberReader
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.user.application.dto.response.UserMyPageResponse
import com.weeth.domain.user.application.mapper.UserMyPageMapper
import com.weeth.domain.user.domain.entity.UserProfile
import com.weeth.domain.user.domain.repository.UserReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetUserMyPageQueryService(
    private val userReader: UserReader,
    private val clubMemberReader: ClubMemberReader,
    private val postReader: PostReader,
    private val attendanceReader: AttendanceReader,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val userMyPageMapper: UserMyPageMapper,
) {
    @Transactional(readOnly = true)
    fun getMyPage(
        userId: Long,
        clubId: Long,
    ): UserMyPageResponse {
        val currentMember = clubMemberPolicy.getActiveMember(clubId, userId)
        return getMyPageResponse(
            userId = userId,
            currentClubMemberId = currentMember.id,
            penaltyCount = currentMember.penaltyCount,
            currentProfile = currentMember.userProfile,
        )
    }

    private fun getMyPageResponse(
        userId: Long,
        currentClubMemberId: Long,
        penaltyCount: Int,
        currentProfile: UserProfile?,
    ): UserMyPageResponse {
        val user = userReader.getById(userId)
        val clubMembers = clubMemberReader.findAllByUserIdWithClubAndUserProfile(userId)
        val currentClubMemberIds = listOf(currentClubMemberId)
        val postCount = countPosts(currentClubMemberIds)
        val attendedSessionCount = countAttendedSessions(currentClubMemberIds)
        val usingProfileMembers =
            clubMembers.filter {
                it.memberStatus == MemberStatus.ACTIVE && it.userProfile != null
            }

        return userMyPageMapper.toResponse(
            user = user,
            postCount = postCount,
            attendedSessionCount = attendedSessionCount,
            penaltyCount = penaltyCount,
            usingProfileMembers = usingProfileMembers,
            currentProfile = currentProfile,
        )
    }

    private fun countPosts(clubMemberIds: List<Long>): Long =
        if (clubMemberIds.isEmpty()) {
            0L
        } else {
            postReader.countActiveByClubMemberIds(clubMemberIds)
        }

    private fun countAttendedSessions(clubMemberIds: List<Long>): Long =
        if (clubMemberIds.isEmpty()) {
            0L
        } else {
            attendanceReader.countByClubMemberIdsAndStatus(clubMemberIds, AttendanceStatus.ATTEND)
        }
}
