package com.cotalk.domain.exception;

/**
 * 파일 업로드 관련 예외
 */
public class FileUploadException extends DomainException {

    public FileUploadException(String message) {
        super(message);
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }

    public static FileUploadException invalidFileType(String contentType) {
        return new FileUploadException("지원하지 않는 파일 형식입니다: " + contentType);
    }

    public static FileUploadException fileTooLarge(long maxSize) {
        return new FileUploadException("파일 크기가 최대 허용 크기를 초과했습니다. 최대: " + maxSize + " bytes");
    }

    public static FileUploadException uploadFailed(Throwable cause) {
        return new FileUploadException("파일 업로드에 실패했습니다.", cause);
    }
}
