package com.cotalk.domain.port.outbound;

import java.io.InputStream;

/**
 * 파일 저장소 아웃바운드 포트.
 * 파일 업로드, 삭제, 조회를 위한 인터페이스를 정의한다.
 * MinIO, AWS S3 등 다양한 저장소 구현체로 교체 가능하다.
 *
 * @author seunggu.lee
 */
public interface FileStorage {

    /**
     * 파일을 업로드한다.
     *
     * @param inputStream 파일 입력 스트림
     * @param fileName    저장할 파일명 (경로 포함 가능)
     * @param contentType 파일 MIME 타입
     * @param fileSize    파일 크기 (bytes)
     * @return 업로드된 파일의 URL
     */
    String upload(InputStream inputStream, String fileName, String contentType, long fileSize);

    /**
     * 파일을 삭제한다.
     *
     * @param fileName 삭제할 파일명 (경로 포함)
     */
    void delete(String fileName);

    /**
     * 파일 존재 여부를 확인한다.
     *
     * @param fileName 확인할 파일명
     * @return 존재 여부
     */
    boolean exists(String fileName);

    /**
     * 파일 다운로드를 위한 Pre-signed URL을 생성한다.
     *
     * @param fileName          파일명
     * @param expirationMinutes URL 유효 시간 (분)
     * @return Pre-signed URL
     */
    String generatePresignedUrl(String fileName, int expirationMinutes);
}
