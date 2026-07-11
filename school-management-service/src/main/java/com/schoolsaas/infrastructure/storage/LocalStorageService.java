package com.schoolsaas.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class LocalStorageService implements StorageService {

    @Value("${app.storage.local.base-path:./uploads}")
    private String basePath;

    @Override
    public String store(MultipartFile file, String folder) {
        try {
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());
            String extension = StringUtils.getFilenameExtension(fileName);
            String newFileName = UUID.randomUUID().toString() + "." + extension;

            Path targetLocation = Paths.get(basePath).resolve(folder).resolve(newFileName);
            Files.createDirectories(targetLocation.getParent());
            
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/" + folder + "/" + newFileName;
        } catch (IOException e) {
            log.error("Could not store file: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de l'enregistrement du fichier", e);
        }
    }

    @Override
    public void delete(String url) {
        try {
            String relativePath = url.replace("/uploads/", "");
            Path targetFile = Paths.get(basePath).resolve(relativePath);
            Files.deleteIfExists(targetFile);
        } catch (IOException e) {
            log.warn("Could not delete file at {}: {}", url, e.getMessage());
        }
    }
}
