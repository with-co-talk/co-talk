package com.cotalk.domain.service;

import com.cotalk.domain.exception.FileUploadException;
import com.cotalk.domain.port.inbound.file.UploadFileUseCase;
import com.cotalk.domain.port.outbound.FileStorage;

import java.io.BufferedInputStream;
import java.io.IOException;
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
public class UploadFileService implements UploadFileUseCase {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UploadFileService.class);

    public static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/heic",
            "image/heif",
            "video/mp4",
            "video/quicktime",
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
     * <p>
     * MP4/MOV/HEIC/HEIF는 ISO Base Media File Format (ISOBMFF)을 사용하며,
     * 'ftyp' 박스가 offset 4부터 시작한다.
     * </p>
     */
    private static final Map<String, byte[][]> MAGIC_NUMBERS = Map.ofEntries(
            Map.entry("image/jpeg", new byte[][]{{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}}),
            Map.entry("image/png", new byte[][]{{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}}),
            Map.entry("image/gif", new byte[][]{{0x47, 0x49, 0x46, 0x38, 0x37, 0x61}, {0x47, 0x49, 0x46, 0x38, 0x39, 0x61}}),
            // WebP: 'RIFF'(0-3) + 파일크기 4바이트(4-7, 와일드카드) + 'WEBP'(8-11).
            // RIFF만 검사하면 WAV/AVI 등 다른 RIFF 컨테이너가 위조 통과하므로 'WEBP' 마커까지 검증한다.
            Map.entry("image/webp", new byte[][]{{0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50}}),
            Map.entry("application/pdf", new byte[][]{{0x25, 0x50, 0x44, 0x46}}),
            // MP4/MOV: 'ftyp' 박스 (offset 4-7)
            Map.entry("video/mp4", new byte[][]{{0x00, 0x00, 0x00, 0x00, 0x66, 0x74, 0x79, 0x70}}),
            Map.entry("video/quicktime", new byte[][]{{0x00, 0x00, 0x00, 0x00, 0x66, 0x74, 0x79, 0x70}}),
            // HEIC/HEIF: 'ftyp' 박스 (offset 4-7)
            Map.entry("image/heic", new byte[][]{{0x00, 0x00, 0x00, 0x00, 0x66, 0x74, 0x79, 0x70}}),
            Map.entry("image/heif", new byte[][]{{0x00, 0x00, 0x00, 0x00, 0x66, 0x74, 0x79, 0x70}})
    );

    private static final int MAX_MAGIC_NUMBER_LENGTH = 12;

    private final FileStorage fileStorage;
    private final long maxFileSize;

    /**
     * UploadFileService 생성자.
     *
     * @param fileStorage 파일 저장소 인터페이스
     * @param maxFileSize 최대 파일 크기 (바이트)
     */
    public UploadFileService(FileStorage fileStorage, long maxFileSize) {
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

        // BufferedInputStream으로 감싸서 mark/reset 지원 (try-with-resources로 스트림 누수 방지)
        try (BufferedInputStream bufferedStream = new BufferedInputStream(command.inputStream())) {
            validateMagicNumber(bufferedStream, command.contentType());

            String storagePath = generateStoragePath(command.userId(), command.originalFileName());
            String fileUrl = fileStorage.upload(
                    bufferedStream,
                    storagePath,
                    command.contentType(),
                    command.fileSize()
            );

            // storagePath(=저장 객체 키)가 곧 불투명 식별자(object-id)다.
            return new FileUploadResult(
                    storagePath,
                    fileUrl,
                    extractFileName(storagePath),
                    command.contentType(),
                    command.fileSize()
            );
        } catch (IOException e) {
            throw FileUploadException.uploadFailed("파일 업로드 중 스트림 처리 오류가 발생했습니다.");
        }
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
                log.warn("File signature mismatch: contentType={}, actualBytes={}", contentType,
                        bytesToHex(fileHeader, bytesRead));
                throw FileUploadException.invalidFileSignature(contentType);
            }
        } catch (IOException e) {
            throw FileUploadException.uploadFailed("파일 시그니처 검증 중 오류가 발생했습니다.");
        }
    }

    /**
     * 바이트 배열이 특정 시그니처로 시작하는지 확인한다.
     * <p>
     * 0x00 바이트는 와일드카드로 처리되어 모든 값과 매치된다.
     * 이는 ISOBMFF 포맷(MP4/MOV/HEIC/HEIF)에서 box size가 가변적인 경우를 처리하기 위함이다.
     * </p>
     *
     * @param data      검사할 데이터 배열
     * @param signature 매칭할 시그니처 (0x00은 와일드카드)
     * @return 시그니처가 매치되면 true
     */
    private boolean startsWith(byte[] data, byte[] signature) {
        if (data.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            // 0x00은 와일드카드로 어떤 값이든 매치
            if (signature[i] != 0x00 && data[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 바이트 배열을 16진수 문자열로 변환한다 (디버깅용).
     *
     * @param bytes  변환할 바이트 배열
     * @param length 변환할 바이트 수
     * @return 16진수 문자열
     */
    private String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(length, bytes.length); i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString().trim();
    }

    private String generateStoragePath(Long userId, String originalFileName) {
        String extension = extractExtension(originalFileName);
        String uniqueFileName = UUID.randomUUID().toString() + extension;
        return String.format("uploads/%d/%s", userId, uniqueFileName);
    }

    /**
     * 원본 파일명에서 안전한 확장자를 추출한다.
     * <p>
     * object key에 그대로 삽입되므로, 경로 구분자나 {@code ..} 같은 위험 문자가
     * 확장자에 섞여 key prefix가 오염되는 것을 방지한다. 마지막 {@code .} 이후 부분이
     * 영숫자로만 구성된 경우에만 확장자로 인정하고, 그 외에는 확장자를 부여하지 않는다.
     * </p>
     *
     * @param fileName 원본 파일명 (신뢰 불가 입력)
     * @return {@code .ext} 형태의 안전한 확장자, 유효하지 않으면 빈 문자열
     */
    private String extractExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        String candidate = fileName.substring(lastDotIndex + 1);
        // 영숫자로만 구성된 확장자만 허용 (슬래시, '..', 공백 등 위생 문자 차단)
        if (!candidate.matches("[A-Za-z0-9]+")) {
            return "";
        }
        return "." + candidate;
    }

    private String extractFileName(String path) {
        int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            return path;
        }
        return path.substring(lastSlashIndex + 1);
    }
}
