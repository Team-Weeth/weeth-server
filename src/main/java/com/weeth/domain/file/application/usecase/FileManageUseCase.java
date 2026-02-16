package com.weeth.domain.file.application.usecase;

import jakarta.transaction.Transactional;
import com.weeth.domain.file.application.dto.response.UrlResponse;
import com.weeth.domain.file.domain.service.PreSignedService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileManageUseCase {

    private final PreSignedService preSignedService;

    public List<UrlResponse> getUrl(List<String> fileNames) {
        return fileNames.stream()
                .map(preSignedService::generateUrl)
                .toList();
    }
}
