package com.weeth.domain.board.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.weeth.domain.board.domain.entity.enums.Category;
import com.weeth.domain.board.domain.entity.enums.Part;
import com.weeth.domain.comment.application.dto.CommentDTO;
import com.weeth.domain.file.application.dto.request.FileSaveRequest;
import com.weeth.domain.file.application.dto.response.FileResponse;
import com.weeth.domain.user.domain.entity.enums.Position;
import com.weeth.domain.user.domain.entity.enums.Role;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

public class PostDTO {

    @Builder
    public record Save(
            @NotBlank(message = "제목 입력은 필수입니다.") String title,
            @NotBlank(message = "내용 입력은 필수입니다.") String content,
            @NotNull Category category,
            String studyName,
            int week,
            @NotNull Part part,
            @NotNull Integer cardinalNumber,
            @Valid List<@NotNull FileSaveRequest> files
    ) {
    }

    @Builder
    public record SaveEducation(
            @NotNull String title,
            @NotNull String content,
            @NotNull List<Part> parts,
            @NotNull Integer cardinalNumber,
            @Valid List<@NotNull FileSaveRequest> files
    ) {
    }

    @Builder
    public record SaveResponse(
            @Schema(description = "게시글 생성시 응답", example = "1")
            long id
    ) {
    }

    @Builder
    public record Update(
            String title,
            String content,
            String studyName,
            Integer week,
            Part part,
            Integer cardinalNumber,
            @Valid List<FileSaveRequest> files
    ) {
    }

    @Builder
    public record UpdateEducation(
            String title,
            String content,
            List<Part> parts,
            Integer cardinalNumber,
            @Valid List<FileSaveRequest> files
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
            String studyName,
            Integer week,
            Integer cardinalNumber,
            Part part,
            List<Part> parts,
            LocalDateTime time,
            Integer commentCount,
            List<CommentDTO.Response> comments,
            List<FileResponse> fileUrls
    ) {
    }

    @Builder
    public record ResponseAll(
            Long id,
            String name,
            Part part,
            Position position,
            Role role,
            String title,
            String content,
            String studyName,
            int week,
            LocalDateTime time,
            Integer commentCount,
            boolean hasFile,
            boolean isNew
    ) {
    }

    @Builder
    public record ResponseEducationAll(
            Long id,
            String name,
            List<Part> parts,
            Position position,
            Role role,
            String title,
            String content,
            LocalDateTime time,
            Integer commentCount,
            boolean hasFile,
            boolean isNew
    ) {
    }

    public record ResponseStudyNames(
            List<String> studyNames
    ) {
    }
}
