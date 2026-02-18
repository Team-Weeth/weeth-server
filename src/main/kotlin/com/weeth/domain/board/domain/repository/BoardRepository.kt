package com.weeth.domain.board.domain.repository

import com.weeth.domain.board.domain.entity.Board
import org.springframework.data.jpa.repository.JpaRepository

interface BoardRepository : JpaRepository<Board, Long>
