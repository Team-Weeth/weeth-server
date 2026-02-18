package com.weeth.domain.board.application.dto;

import com.weeth.domain.comment.application.dto.response.CommentResponse;
import com.weeth.domain.file.application.dto.request.FileSaveRequest;
import com.weeth.domain.file.application.dto.response.FileResponse;
import com.weeth.domain.user.domain.entity.enums.Position;
import com.weeth.domain.user.domain.entity.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class NoticeDTO {

    @Builder
    public record Save(
            @NotNull String title,
            @NotNull String content,
            @Valid List<@NotNull FileSaveRequest> files
    ) {
    }

    @Builder
    public record Update(
            @NotNull String title,
            @NotNull String content,
            @Valid List<@NotNull FileSaveRequest> files
    ) {
    }

    @Builder
    public record Response(
            Long id,
            String name,
            Position position,
            Role role,
            String title,
            String content,
            LocalDateTime time, //createdAt
            Integer commentCount,
            List<CommentResponse> comments,
            List<FileResponse> fileUrls
    ) {
    }

    @Builder
    public record ResponseAll(
            Long id,
            String name,
            Position position,
            Role role,
            String title,
            String content,
            LocalDateTime time,//modifiedAt
            Integer commentCount,
            boolean hasFile
    ) {
    }

    @Builder
    public record SaveResponse(
            @Schema(description = "공지사항 생성 응답", example = "1")
            long id
    ) {
    }

}
