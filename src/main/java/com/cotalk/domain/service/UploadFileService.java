package com.cotalk.domain.service;

import com.cotalk.domain.exception.FileUploadException;
import com.cotalk.domain.port.inbound.file.UploadFileUseCase;
import com.cotalk.domain.port.outbound.FileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

/**
 * 파일 업로드 서비스.
 * <p>
 * 파일 업로드 관련 비즈니스 로직을 처리한다.
 * 파일 크기 및 타입 검증, 저장 경로 생성, 파일 저장소 연동 등의 역할을 담당한다.
 * </p>
 * <p>
 * 지원하는 파일 형식:
 * <ul>
 *   <li>이미지: JPEG, PNG, GIF, WebP</li>
 *   <li>문서: PDF, DOC, DOCX, XLS, XLSX, TXT</li>
 * </ul>
 * </p>
 *
 * @author seunggu.lee
 * @see UploadFileUseCase
 * @see FileStorage
 */
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

    /**
     * UploadFileService 생성자.
     *
     * @param fileStorage 파일 저장소 인터페이스
     * @param maxFileSize 최대 파일 크기 (바이트 단위, 기본값: 10MB)
     */
    public UploadFileService(
            FileStorage fileStorage,
            @Value("${file.upload.max-size:10485760}") long maxFileSize) {
        this.fileStorage = fileStorage;
        this.maxFileSize = maxFileSize;
    }

    /**
     * 파일을 업로드한다.
     * <p>
     * 파일 크기와 타입을 검증한 후, 고유한 저장 경로를 생성하여 파일을 저장한다.
     * </p>
     *
     * @param command 파일 업로드 명령 객체 (파일 정보 포함)
     * @return 업로드 결과 (파일 URL, 파일명, 타입, 크기 포함)
     * @throws FileUploadException 파일 크기 초과 또는 허용되지 않는 파일 타입인 경우
     */
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
