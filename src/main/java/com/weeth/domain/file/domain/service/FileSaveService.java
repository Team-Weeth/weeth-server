package com.weeth.domain.file.domain.service;

import com.weeth.domain.file.domain.entity.File;
import com.weeth.domain.file.domain.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileSaveService {
    private final FileRepository fileRepository;

    public void save(File file) {
        fileRepository.save(file);
    }

    public void save(List<File> files) {
        fileRepository.saveAll(files);
    }
}
