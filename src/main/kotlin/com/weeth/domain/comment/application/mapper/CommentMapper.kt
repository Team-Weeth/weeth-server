package com.weeth.domain.comment.application.mapper

import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.comment.application.dto.response.CommentResponse
import com.weeth.domain.comment.domain.entity.Comment
import com.weeth.domain.file.application.dto.response.FileResponse
import com.weeth.domain.file.domain.port.FileAccessUrlPort
import com.weeth.domain.user.application.dto.response.UserInfo
import org.springframework.stereotype.Component

@Component
class CommentMapper(
    private val fileAccessUrlPort: FileAccessUrlPort,
) {
    fun toCommentDto(
        comment: Comment,
        children: List<CommentResponse>,
        fileUrls: List<FileResponse>,
    ): CommentResponse =
        CommentResponse(
            id = comment.id,
            author = toAuthorInfo(comment.clubMember),
            content = comment.content,
            time = comment.createdAt,
            fileUrls = fileUrls,
            children = children,
        )

    private fun toAuthorInfo(member: ClubMember): UserInfo =
        UserInfo.ofClubMemberProfile(
            clubMember = member,
            profileName = member.userProfile?.name ?: member.user.name,
            resolvedProfileImageUrl = resolveProfileImage(member),
        )

    private fun resolveProfileImage(member: ClubMember): String? {
        val storageKey = member.userProfile?.profileImageStorageKey ?: member.profileImageStorageKey
        return storageKey?.let { fileAccessUrlPort.resolve(it) }
    }
}
