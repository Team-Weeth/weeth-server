package com.weeth.domain.user.application.dto.response

import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.club.domain.enums.MemberRole
import com.weeth.domain.club.domain.enums.MemberStatus
import com.weeth.domain.user.domain.entity.User
import io.swagger.v3.oas.annotations.media.Schema

data class UserInfo(
    @field:Schema(description = "사용자 ID", example = "1")
    val id: Long,
    @field:Schema(description = "이름", example = "홍길동")
    val name: String,
    @field:Schema(description = "프로필 이미지 URL")
    val profileImageUrl: String?,
    @field:Schema(description = "동아리 내 권한", example = "USER")
    val role: MemberRole,
) {
    companion object {
        const val ANONYMOUS_USER_NAME = "탈퇴한 사용자"

        /**
         * 본인 정보 응답 전용 팩토리. 사용자가 자기 자신을 조회하는 흐름에서만 호출한다.
         *
         * 게시글/댓글/대시보드 등 동아리 활동의 작성자 매핑에는 익명화가 적용되는
         * [ofClubMember]를 사용해야 한다. LEFT 멤버 이름·프로필이 그대로 노출될 수 있어
         * 본 팩토리를 작성자 매핑에 사용하지 말 것.
         */
        fun ofSelf(
            user: User,
            role: MemberRole,
            resolvedProfileImageUrl: String?,
        ) = UserInfo(
            id = user.id,
            name = user.name,
            profileImageUrl = resolvedProfileImageUrl,
            role = role,
        )

        // 탈퇴(LEFT) 멤버는 이름·프로필을 노출하지 않고 익명 라벨로 치환한다.
        fun ofClubMember(
            clubMember: ClubMember,
            resolvedProfileImageUrl: String?,
        ): UserInfo =
            ofClubMemberProfile(
                clubMember = clubMember,
                profileName = clubMember.user.name,
                resolvedProfileImageUrl = resolvedProfileImageUrl,
            )

        fun ofClubMemberProfile(
            clubMember: ClubMember,
            profileName: String,
            resolvedProfileImageUrl: String?,
        ): UserInfo {
            val isLeft = clubMember.memberStatus == MemberStatus.LEFT
            return UserInfo(
                id = clubMember.user.id,
                name = if (isLeft) ANONYMOUS_USER_NAME else profileName,
                profileImageUrl = if (isLeft) null else resolvedProfileImageUrl,
                role = clubMember.memberRole,
            )
        }
    }
}
