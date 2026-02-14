package leets.weeth.domain.file.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

public record FileUpdateRequest(
        @NotNull Long fileId,
        @NotBlank String fileName,
        @NotBlank @URL String fileUrl
) {
}
