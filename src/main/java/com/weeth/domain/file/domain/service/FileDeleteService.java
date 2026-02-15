package com.weeth.domain.file.domain.service;

import com.weeth.domain.file.domain.entity.File;
import com.weeth.domain.file.domain.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileDeleteService {

    private final FileRepository fileRepository;

    public void delete(File file) {
        fileRepository.delete(file);
    }

    public void delete(List<File> files) {
        fileRepository.deleteAll(files);
    }
}
