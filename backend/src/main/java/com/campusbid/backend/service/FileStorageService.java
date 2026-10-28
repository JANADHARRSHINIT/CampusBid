package com.campusbid.backend.service;

import com.campusbid.backend.exception.FileStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    public FileStorageService(@Value("${campusbid.upload.dir:uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create directory for uploaded files at " + this.fileStorageLocation, ex);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Failed to store empty file.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileStorageException("File size exceeds 5MB limit. Size: " + (file.getSize() / (1024 * 1024)) + "MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new FileStorageException("Invalid image type: " + contentType + ". Allowed types: JPEG, PNG, WEBP, GIF.");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex + 1).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new FileStorageException("Invalid image file extension: ." + extension + ". Allowed: .jpg, .jpeg, .png, .webp, .gif");
        }

        try {
            // Generate unique filename to avoid collisions
            String uniqueFilename = UUID.randomUUID() + "_" + originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFilename);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            // Return relative public path served by WebMvc resource handler
            return "/uploads/" + uniqueFilename;
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + originalFilename + ". Please try again!", ex);
        }
    }

    public List<String> storeFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }
        if (files.length > 5) {
            throw new FileStorageException("Maximum 5 photos allowed per listing.");
        }

        List<String> storedUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                storedUrls.add(storeFile(file));
            }
        }
        return storedUrls;
    }

    public Path getFileStorageLocation() {
        return fileStorageLocation;
    }
}
