package com.cotalk.domain.service;

import com.cotalk.domain.exception.FileUploadException;
import com.cotalk.domain.port.inbound.file.UploadFileUseCase;
import com.cotalk.domain.port.outbound.FileStorage;
import com.cotalk.infrastructure.config.properties.FileUploadProperties;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
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

    /**
     * 파일 매직넘버(시그니처) 정의.
     * 각 파일 형식의 첫 바이트들을 기반으로 실제 파일 타입을 검증한다.
     */
    private static final Map<String, byte[][]> MAGIC_NUMBERS = Map.of(
            "image/jpeg", new byte[][]{{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}},
            "image/png", new byte[][]{{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}},
            "image/gif", new byte[][]{{0x47, 0x49, 0x46, 0x38, 0x37, 0x61}, {0x47, 0x49, 0x46, 0x38, 0x39, 0x61}},
            "image/webp", new byte[][]{{0x52, 0x49, 0x46, 0x46}},
            "application/pdf", new byte[][]{{0x25, 0x50, 0x44, 0x46}}
    );

    private static final int MAX_MAGIC_NUMBER_LENGTH = 8;

    private final FileStorage fileStorage;
    private final long maxFileSize;

    /**
     * UploadFileService 생성자.
     *
     * @param fileStorage          파일 저장소 인터페이스
     * @param fileUploadProperties 파일 업로드 설정 프로퍼티
     */
    public UploadFileService(FileStorage fileStorage, FileUploadProperties fileUploadProperties) {
        this.fileStorage = fileStorage;
        this.maxFileSize = fileUploadProperties.maxSize();
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

        // BufferedInputStream으로 감싸서 mark/reset 지원
        BufferedInputStream bufferedStream = new BufferedInputStream(command.inputStream());
        validateMagicNumber(bufferedStream, command.contentType());

        String storagePath = generateStoragePath(command.userId(), command.originalFileName());
        String fileUrl = fileStorage.upload(
                bufferedStream,
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

    /**
     * 파일의 매직넘버(시그니처)를 검증한다.
     * <p>
     * Content-Type으로 선언된 파일 형식과 실제 파일 바이트가 일치하는지 확인하여
     * 파일 확장자 위조 공격을 방지한다.
     * </p>
     *
     * @param inputStream 검사할 파일의 입력 스트림 (mark/reset 지원 필요)
     * @param contentType 선언된 Content-Type
     * @throws FileUploadException 매직넘버 검증 실패 시
     */
    private void validateMagicNumber(BufferedInputStream inputStream, String contentType) {
        byte[][] expectedSignatures = MAGIC_NUMBERS.get(contentType);

        // 매직넘버 검증이 정의되지 않은 파일 타입은 Content-Type 검증만으로 통과
        if (expectedSignatures == null) {
            return;
        }

        try {
            inputStream.mark(MAX_MAGIC_NUMBER_LENGTH);
            byte[] fileHeader = new byte[MAX_MAGIC_NUMBER_LENGTH];
            int bytesRead = inputStream.read(fileHeader);
            inputStream.reset();

            if (bytesRead < 0) {
                throw FileUploadException.invalidFileSignature(contentType);
            }

            boolean signatureMatched = false;
            for (byte[] signature : expectedSignatures) {
                if (bytesRead >= signature.length && startsWith(fileHeader, signature)) {
                    signatureMatched = true;
                    break;
                }
            }

            if (!signatureMatched) {
                throw FileUploadException.invalidFileSignature(contentType);
            }
        } catch (IOException e) {
            throw FileUploadException.uploadFailed("파일 시그니처 검증 중 오류가 발생했습니다.");
        }
    }

    /**
     * 바이트 배열이 특정 시그니처로 시작하는지 확인한다.
     */
    private boolean startsWith(byte[] data, byte[] signature) {
        if (data.length < signature.length) {
            return false;
        }
        return Arrays.equals(Arrays.copyOf(data, signature.length), signature);
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
