package com.weeth.domain.account.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import com.weeth.domain.file.application.dto.request.FileSaveRequest;
import com.weeth.domain.file.application.dto.response.FileResponse;

import java.time.LocalDate;
import java.util.List;

public class ReceiptDTO {

    public record Response(
            Long id,
            String description,
            String source,
            Integer amount,
            LocalDate date,
            List<FileResponse> fileUrls
    ) {
    }

    public record Save(
            String description,
            String source,
            @NotNull Integer amount,
            @NotNull LocalDate date,
            @NotNull Integer cardinal,
            @Valid List<@NotNull FileSaveRequest> files
    ) {
    }

    public record Update(
            String description,
            String source,
            @NotNull Integer amount,
            @NotNull LocalDate date,
            @NotNull Integer cardinal,
            @Valid List<@NotNull FileSaveRequest> files
    ) {
    }
}
