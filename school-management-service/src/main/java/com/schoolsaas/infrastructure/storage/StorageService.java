package com.schoolsaas.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String store(MultipartFile file, String folder);
    void delete(String url);
}
