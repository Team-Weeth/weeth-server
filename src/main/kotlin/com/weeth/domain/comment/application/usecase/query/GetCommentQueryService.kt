package com.weeth.domain.comment.application.usecase.query

import com.weeth.domain.club.domain.entity.ClubMember
import com.weeth.domain.comment.application.dto.response.CommentResponse
import com.weeth.domain.comment.application.mapper.CommentMapper
import com.weeth.domain.comment.domain.entity.Comment
import com.weeth.domain.file.application.mapper.FileMapper
import com.weeth.domain.file.domain.entity.File
import com.weeth.domain.file.domain.enums.FileOwnerType
import com.weeth.domain.file.domain.repository.FileReader
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetCommentQueryService(
    private val fileReader: FileReader,
    private val fileMapper: FileMapper,
    private val commentMapper: CommentMapper,
) {
    /**
     * Comment 리스트를 받아 자식, 부모 관계 트리를 형성하는 메서드
     */
    fun toCommentTreeResponses(
        comments: List<Comment>,
        memberMap: Map<Long, ClubMember>,
    ): List<CommentResponse> {
        if (comments.isEmpty()) {
            return emptyList()
        }

        val commentIds: List<Long> = comments.map { it.id }
        val filesByCommentId: Map<Long, List<File>> =
            fileReader
                .findAll(FileOwnerType.COMMENT, commentIds)
                .groupBy { it.ownerId }

        val childrenByParentId: Map<Long, List<Comment>> =
            comments
                .filter { it.parent != null }
                .groupBy { requireNotNull(it.parent).id }

        return comments
            .filter { it.parent == null }
            .map { mapToCommentResponse(it, childrenByParentId, filesByCommentId, memberMap) }
    }

    private fun mapToCommentResponse(
        comment: Comment,
        childrenByParentId: Map<Long, List<Comment>>,
        filesByCommentId: Map<Long, List<File>>,
        memberMap: Map<Long, ClubMember>,
    ): CommentResponse {
        val children =
            childrenByParentId[comment.id]
                ?.map { mapToCommentResponse(it, childrenByParentId, filesByCommentId, memberMap) }
                ?: emptyList()

        val files =
            filesByCommentId[comment.id]
                ?.map(fileMapper::toFileResponse)
                ?: emptyList()

        val authorMember = memberMap.getValue(comment.user.id)
        return commentMapper.toCommentDto(comment, authorMember, children, files)
    }
}
