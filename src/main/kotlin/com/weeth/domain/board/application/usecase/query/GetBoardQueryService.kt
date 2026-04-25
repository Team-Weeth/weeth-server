package com.weeth.domain.board.application.usecase.query

import com.weeth.domain.board.application.dto.response.BoardDetailResponse
import com.weeth.domain.board.application.dto.response.BoardListResponse
import com.weeth.domain.board.application.dto.response.BoardNameDuplicateResponse
import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.mapper.BoardMapper
import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.board.domain.repository.PostRepository
import com.weeth.domain.club.domain.service.ClubMemberPolicy
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetBoardQueryService(
    private val boardRepository: BoardRepository,
    private val postRepository: PostRepository,
    private val clubMemberPolicy: ClubMemberPolicy,
    private val clubPermissionPolicy: ClubPermissionPolicy,
    private val boardMapper: BoardMapper,
) {
    fun findBoards(
        clubId: Long,
        userId: Long,
    ): List<BoardListResponse> {
        val member = clubMemberPolicy.getActiveMember(clubId, userId)

        val realBoards =
            boardRepository
                .findAllByClubIdAndIsDeletedFalseOrderByDisplayOrderAscIdAsc(clubId)
                .filter { it.isAccessibleBy(member.memberRole) }

        // 공지사항 고정 첫 번째, 전체(가상) 두 번째, 나머지는 displayOrder 순
        val (noticeList, otherList) = realBoards.partition { it.type == BoardType.NOTICE }
        val noticeBoards = noticeList.map(boardMapper::toListResponse)
        val otherBoards = otherList.map(boardMapper::toListResponse)

        return noticeBoards + VIRTUAL_ALL_BOARD + otherBoards
    }

    fun findBoardDetailForAdmin(
        clubId: Long,
        userId: Long,
        boardId: Long,
    ): BoardDetailResponse {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val board = boardRepository.findByIdAndClubId(boardId, clubId) ?: throw BoardNotFoundException()
        val postCount =
            postRepository
                .countActivePostsByBoardIds(listOf(boardId))
                .firstOrNull()
                ?.postCount
                ?.toInt() ?: 0

        return boardMapper.toDetailResponseForAdmin(board, postCount)
    }

    fun findAllBoardsForAdmin(
        clubId: Long,
        userId: Long,
    ): List<BoardDetailResponse> {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val boards = boardRepository.findAllByClubIdOrderByDisplayOrderAscIdAsc(clubId)
        val boardIds = boards.map { it.id }
        val postCountMap =
            if (boardIds.isEmpty()) {
                emptyMap()
            } else {
                postRepository.countActivePostsByBoardIds(boardIds).associate { it.boardId to it.postCount.toInt() }
            }

        val (noticeList, otherList) = boards.partition { it.type == BoardType.NOTICE }
        val noticeBoards = noticeList.map { boardMapper.toDetailResponseForAdmin(it, postCountMap[it.id] ?: 0) }
        val otherBoards = otherList.map { boardMapper.toDetailResponseForAdmin(it, postCountMap[it.id] ?: 0) }
        val totalPostCount = postCountMap.values.sum()

        return noticeBoards + virtualAllBoardForAdmin(totalPostCount) + otherBoards
    }

    fun checkBoardNameDuplicate(
        clubId: Long,
        userId: Long,
        name: String,
        boardId: Long? = null,
    ): BoardNameDuplicateResponse {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val duplicated =
            if (boardId == null) {
                boardRepository.existsByClubIdAndNameAndIsDeletedFalse(clubId, name)
            } else {
                boardRepository.existsByClubIdAndNameAndIsDeletedFalseAndIdNot(clubId, name, boardId)
            }

        return BoardNameDuplicateResponse(duplicated = duplicated)
    }

    companion object {
        private val VIRTUAL_ALL_BOARD = BoardListResponse(id = null, name = "전체", type = BoardType.ALL)

        private fun virtualAllBoardForAdmin(totalPostCount: Int) =
            BoardDetailResponse(
                id = null,
                name = "전체",
                description = null,
                type = BoardType.ALL,
                commentEnabled = null,
                writePermission = null,
                isPrivate = null,
                displayOrder = null,
                postCount = totalPostCount,
                isDeleted = null,
            )
    }
}
