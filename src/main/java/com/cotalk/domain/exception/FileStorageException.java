package com.cotalk.domain.exception;

/**
 * 파일 저장소 관련 예외.
 * 파일 삭제, 조회 등 저장소 작업 중 발생하는 오류를 처리한다.
 *
 * @author seunggu.lee
 */
public class FileStorageException extends DomainException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 파일 삭제 실패 시 발생하는 예외.
     *
     * @param fileName 삭제 실패한 파일명
     * @param cause 원인 예외
     * @return FileStorageException
     */
    public static FileStorageException deleteFailed(String fileName, Throwable cause) {
        return new FileStorageException("파일 삭제에 실패했습니다: " + fileName, cause);
    }

    /**
     * 파일 조회 실패 시 발생하는 예외.
     *
     * @param fileName 조회 실패한 파일명
     * @return FileStorageException
     */
    public static FileStorageException notFound(String fileName) {
        return new FileStorageException("파일을 찾을 수 없습니다: " + fileName);
    }
}
