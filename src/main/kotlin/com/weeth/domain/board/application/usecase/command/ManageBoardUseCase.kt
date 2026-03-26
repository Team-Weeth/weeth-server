package com.weeth.domain.board.application.usecase.command

import com.weeth.domain.board.application.dto.request.CreateBoardRequest
import com.weeth.domain.board.application.dto.request.ReorderBoardsRequest
import com.weeth.domain.board.application.dto.request.UpdateBoardRequest
import com.weeth.domain.board.application.dto.response.BoardDetailResponse
import com.weeth.domain.board.application.exception.BoardCreateLockTimeoutException
import com.weeth.domain.board.application.exception.BoardLimitExceededException
import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.exception.BoardNotInClubException
import com.weeth.domain.board.application.exception.DeletedBoardNotReorderableException
import com.weeth.domain.board.application.exception.DuplicateBoardIdException
import com.weeth.domain.board.application.exception.DuplicateBoardNameException
import com.weeth.domain.board.application.exception.FixedBoardNotDeletableException
import com.weeth.domain.board.application.exception.FixedBoardNotRenamableException
import com.weeth.domain.board.application.exception.FixedBoardNotReorderableException
import com.weeth.domain.board.application.mapper.BoardMapper
import com.weeth.domain.board.domain.entity.Board
import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.board.domain.vo.BoardConfig
import com.weeth.domain.club.domain.repository.ClubReader
import com.weeth.domain.club.domain.service.ClubPermissionPolicy
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManageBoardUseCase(
    private val boardRepository: BoardRepository,
    private val boardMapper: BoardMapper,
    private val clubReader: ClubReader,
    private val clubPermissionPolicy: ClubPermissionPolicy,
) {
    @Transactional
    fun create(
        clubId: Long,
        request: CreateBoardRequest,
        userId: Long,
    ): BoardDetailResponse {
        clubPermissionPolicy.requireAdmin(clubId, userId)

        val club =
            try {
                clubReader.getClubByIdForUpdate(clubId)
            } catch (_: PessimisticLockingFailureException) {
                throw BoardCreateLockTimeoutException()
            }

        if (boardRepository.countByClubIdAndTypeNotAndIsDeletedFalse(clubId, BoardType.NOTICE) >= MAX_BOARD_COUNT) {
            throw BoardLimitExceededException()
        }

        if (boardRepository.existsByClubIdAndNameAndIsDeletedFalse(
                clubId,
                request.name,
            )
        ) {
            throw DuplicateBoardNameException()
        }

        val nextOrder = boardRepository.findMaxActiveDisplayOrderByClubId(clubId) + 1
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
            if (board.type == BoardType.NOTICE) throw FixedBoardNotRenamableException()
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
        if (board.type == BoardType.NOTICE) throw FixedBoardNotDeletableException()
        val maxOrder = boardRepository.findMaxDisplayOrderByClubId(clubId)
        board.markDeleted()
        board.reorder(maxOrder + 1)
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

        val allBoards = boardRepository.findAllByClubIdOrderByDisplayOrderAscIdAsc(clubId)
        val (activeBoards, deletedBoards) = allBoards.partition { !it.isDeleted }

        // 삭제된 게시판 ID가 요청에 포함되면 명확한 에러 반환
        val deletedIds = deletedBoards.mapTo(mutableSetOf()) { it.id }
        if (uniqueIds.any { it in deletedIds }) throw DeletedBoardNotReorderableException()

        val (fixedBoards, reorderableBoards) = activeBoards.partition { it.type == BoardType.NOTICE }

        // 고정 게시판 ID가 요청에 포함되면 명확한 에러 반환
        val fixedIds = fixedBoards.mapTo(mutableSetOf()) { it.id }
        if (uniqueIds.any { it in fixedIds }) throw FixedBoardNotReorderableException()

        val boardById = reorderableBoards.associateBy { it.id }
        if (uniqueIds.any { it !in boardById }) throw BoardNotInClubException()

        // 요청된 게시판들의 현재 displayOrder 슬롯을 정렬 후 재배분 (부분 재정렬 시 충돌 방지)
        val slots = request.boardIds.map { boardById.getValue(it).displayOrder }.sorted()
        request.boardIds.forEachIndexed { index, boardId ->
            boardById.getValue(boardId).reorder(slots[index])
        }
    }

    private fun findBoard(boardId: Long): Board =
        boardRepository.findByIdAndIsDeletedFalse(boardId) ?: throw BoardNotFoundException()

    companion object {
        private const val MAX_BOARD_COUNT = 3L
    }
}
