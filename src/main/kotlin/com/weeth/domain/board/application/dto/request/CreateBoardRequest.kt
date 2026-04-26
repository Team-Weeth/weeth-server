package com.weeth.domain.board.application.dto.request

import com.weeth.domain.board.domain.enums.BoardType
import com.weeth.domain.club.domain.enums.MemberRole
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateBoardRequest(
    @field:Schema(description = "게시판 이름", example = "공지사항")
    @field:NotBlank
    @field:Size(max = 20)
    val name: String,
    @field:Schema(description = "게시판 설명", example = "공지사항 게시판입니다.")
    @field:NotBlank
    @field:Size(max = 30)
    val description: String,
    @field:Schema(description = "게시판 타입", example = "NOTICE")
    @field:NotNull
    var type: BoardType,
    @field:Schema(description = "댓글 허용 여부", example = "true")
    val commentEnabled: Boolean = true,
    @field:Schema(description = "게시글 작성 권한", example = "USER")
    val writePermission: MemberRole = MemberRole.USER,
    @field:Schema(description = "비공개 게시판 여부", example = "false")
    val isPrivate: Boolean = false,
)
