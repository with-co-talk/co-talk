package com.cotalk.domain.service;

import com.cotalk.domain.exception.FileUploadException;
import com.cotalk.domain.port.inbound.UploadFileUseCase;
import com.cotalk.domain.port.outbound.FileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Service
public class UploadFileService implements UploadFileUseCase {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain"
    );

    private final FileStorage fileStorage;
    private final long maxFileSize;

    public UploadFileService(
            FileStorage fileStorage,
            @Value("${file.upload.max-size:10485760}") long maxFileSize) {
        this.fileStorage = fileStorage;
        this.maxFileSize = maxFileSize;
    }

    @Override
    public FileUploadResult uploadFile(FileUploadCommand command) {
        validateFileSize(command.fileSize());
        validateContentType(command.contentType());

        String storagePath = generateStoragePath(command.userId(), command.originalFileName());
        String fileUrl = fileStorage.upload(
                command.inputStream(),
                storagePath,
                command.contentType(),
                command.fileSize()
        );

        return new FileUploadResult(
                fileUrl,
                extractFileName(storagePath),
                command.contentType(),
                command.fileSize()
        );
    }

    private void validateFileSize(long fileSize) {
        if (fileSize > maxFileSize) {
            throw FileUploadException.fileTooLarge(maxFileSize);
        }
    }

    private void validateContentType(String contentType) {
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw FileUploadException.invalidFileType(contentType);
        }
    }

    private String generateStoragePath(Long userId, String originalFileName) {
        String extension = extractExtension(originalFileName);
        String uniqueFileName = UUID.randomUUID().toString() + extension;
        return String.format("uploads/%d/%s", userId, uniqueFileName);
    }

    private String extractExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex);
    }

    private String extractFileName(String path) {
        int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            return path;
        }
        return path.substring(lastSlashIndex + 1);
    }
}
