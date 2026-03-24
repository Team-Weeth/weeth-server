package com.weeth.domain.board.application.usecase.command

import com.weeth.domain.board.application.dto.request.CreateBoardRequest
import com.weeth.domain.board.application.dto.request.ReorderBoardsRequest
import com.weeth.domain.board.application.dto.request.UpdateBoardRequest
import com.weeth.domain.board.application.dto.response.BoardDetailResponse
import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.exception.BoardNotInClubException
import com.weeth.domain.board.application.exception.DuplicateBoardIdException
import com.weeth.domain.board.application.exception.DuplicateBoardNameException
import com.weeth.domain.board.application.mapper.BoardMapper
import com.weeth.domain.board.domain.entity.Board
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.board.domain.vo.BoardConfig
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManageBoardUseCase(
    private val boardRepository: BoardRepository,
    private val boardMapper: BoardMapper,
    private val clubReader: ClubReader,
    private val clubPermissionPolicy: ClubPermissionPolicy,
) {
    /**
     * 게시판 생성 API, 커스텀한 게시판 생성 가능
     * TODO: MVP, 무료의 경우엔 개수 제한. 공지사항 제외
     */
    @Transactional
    fun create(
        clubId: Long,
        request: CreateBoardRequest,
        userId: Long,
    ): BoardDetailResponse {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val club = clubReader.getClubById(clubId)

        if (boardRepository.existsByClubIdAndNameAndIsDeletedFalse(
                clubId,
                request.name,
            )
        ) {
            throw DuplicateBoardNameException()
        }

        val nextOrder = boardRepository.findMaxDisplayOrderByClubId(clubId) + 1
        val board =
            Board(
                club = club,
                name = request.name,
                type = request.type,
                config =
                    BoardConfig(
                        commentEnabled = request.commentEnabled,
                        writePermission = request.writePermission,
                        isPrivate = request.isPrivate,
                    ),
            )
        board.reorder(nextOrder)

        val savedBoard = boardRepository.save(board)
        return boardMapper.toDetailResponse(savedBoard)
    }

    @Transactional
    fun update(
        clubId: Long,
        boardId: Long,
        request: UpdateBoardRequest,
        userId: Long,
    ): BoardDetailResponse {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val board = findBoard(boardId)
        if (board.club.id != clubId) throw BoardNotFoundException()

        request.name?.let {
            if (boardRepository.existsByClubIdAndNameAndIsDeletedFalseAndIdNot(
                    clubId,
                    it,
                    boardId,
                )
            ) {
                throw DuplicateBoardNameException()
            }
            board.rename(it)
        }

        // BoardConfig는 불변 VO이므로 개별 필드 수정이 불가능하여 copy()로 새 객체를 만들어 통째로 교체한다. null이면 기존 값을 명시적으로 채운다.
        // 바깥 if 문은 config 관련 필드가 전부 null인 요청에서 불필요한 VO 생성을 방지한다.
        if (request.commentEnabled != null || request.writePermission != null || request.isPrivate != null) {
            board.updateConfig(
                board.config.copy(
                    commentEnabled = request.commentEnabled ?: board.config.commentEnabled,
                    writePermission = request.writePermission ?: board.config.writePermission,
                    isPrivate = request.isPrivate ?: board.config.isPrivate,
                ),
            )
        }

        return boardMapper.toDetailResponse(board)
    }

    @Transactional
    fun delete(
        clubId: Long,
        boardId: Long,
        userId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)
        val board = findBoard(boardId)

        if (board.club.id != clubId) throw BoardNotFoundException()
        board.markDeleted()
    }

    @Transactional
    fun reorder(
        clubId: Long,
        request: ReorderBoardsRequest,
        userId: Long,
    ) {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val uniqueIds = request.boardIds.toSet()
        if (uniqueIds.size != request.boardIds.size) throw DuplicateBoardIdException()

        // 클럽의 모든 활성 게시판이 요청에 포함되어야 순서가 일관성 있게 유지됨
        val allActiveBoards = boardRepository.findAllByClubIdAndIsDeletedFalseOrderByDisplayOrderAscIdAsc(clubId)
        if (allActiveBoards.size != uniqueIds.size) throw BoardNotInClubException()

        val boardById = allActiveBoards.associateBy { it.id }
        if (!boardById.keys.containsAll(uniqueIds)) throw BoardNotInClubException()

        request.boardIds.forEachIndexed { index, boardId ->
            boardById.getValue(boardId).reorder(index)
        }
    }

    private fun findBoard(boardId: Long): Board =
        boardRepository.findByIdAndIsDeletedFalse(boardId) ?: throw BoardNotFoundException()
}
