package com.cotalk.domain.exception;

/**
 * 파일 업로드 관련 예외.
 *
 * @author seunggu.lee
 */
public class FileUploadException extends DomainException {

    public FileUploadException(String message) {
        super(message, "FILE_UPLOAD_ERROR", HttpStatusHint.BAD_REQUEST);
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, "FILE_UPLOAD_ERROR", HttpStatusHint.BAD_REQUEST, cause);
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

    /**
     * 파일 시그니처(매직넘버) 검증 실패 시 발생하는 예외.
     *
     * @param contentType 선언된 Content-Type
     * @return FileUploadException
     */
    public static FileUploadException invalidFileSignature(String contentType) {
        return new FileUploadException(
                "파일 시그니처가 선언된 Content-Type과 일치하지 않습니다: " + contentType +
                ". 파일이 손상되었거나 확장자가 실제 형식과 다를 수 있습니다.");
    }

    /**
     * 파일 업로드 처리 중 오류 발생 시 발생하는 예외.
     *
     * @param message 오류 메시지
     * @return FileUploadException
     */
    public static FileUploadException uploadFailed(String message) {
        return new FileUploadException(message);
    }
}
