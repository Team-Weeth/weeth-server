package leets.weeth.domain.file.test.fixture;

import leets.weeth.domain.board.domain.entity.Notice;
import leets.weeth.domain.file.domain.entity.File;

public class FileTestFixture {
    public static File createFile(Long id, String fileName, String fileUrl){
        return File.builder()
                .id(id)
                .fileName(fileName)
                .fileUrl(fileUrl)
                .build();
    }

    public static File createFile(Long id, String fileName, String fileUrl, Notice notice){
        return File.builder()
                .id(id)
                .fileName(fileName)
                .fileUrl(fileUrl)
                .notice(notice)
                .build();
    }
}
