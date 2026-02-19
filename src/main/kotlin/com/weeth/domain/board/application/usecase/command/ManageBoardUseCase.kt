package com.weeth.domain.board.application.usecase.command

import com.weeth.domain.board.application.dto.request.CreateBoardRequest
import com.weeth.domain.board.application.dto.request.UpdateBoardRequest
import com.weeth.domain.board.application.dto.response.BoardDetailResponse
import com.weeth.domain.board.application.exception.BoardNotFoundException
import com.weeth.domain.board.application.mapper.BoardMapper
import com.weeth.domain.board.domain.entity.Board
import com.weeth.domain.board.domain.repository.BoardRepository
import com.weeth.domain.board.domain.vo.BoardConfig
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ManageBoardUseCase(
    private val boardRepository: BoardRepository,
    private val boardMapper: BoardMapper,
) {
    @Transactional
    fun create(request: CreateBoardRequest): BoardDetailResponse {
        val board =
            Board(
                name = request.name,
                type = request.type,
                config =
                    BoardConfig(
                        commentEnabled = request.commentEnabled,
                        writePermission = request.writePermission,
                        isPrivate = request.isPrivate,
                    ),
            )
        val savedBoard = boardRepository.save(board)
        return boardMapper.toDetailResponse(savedBoard)
    }

    @Transactional
    fun update(
        boardId: Long,
        request: UpdateBoardRequest,
    ): BoardDetailResponse {
        val board = findBoard(boardId)

        request.name?.let { board.rename(it) }

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
    fun delete(boardId: Long) {
        val board = findBoard(boardId)
        board.markDeleted()
    }

    private fun findBoard(boardId: Long): Board =
        boardRepository.findByIdAndIsDeletedFalse(boardId) ?: throw BoardNotFoundException()
}
