package leets.weeth.domain.file.application.dto.response;

public record FileResponse(
        long fileId,
        String fileName,
        String fileUrl
) {
}
