package com.cotalk.domain.port.inbound.file;

import java.io.InputStream;

/**
 * 파일 업로드 유스케이스.
 * 파일 업로드 기능을 제공한다.
 *
 * @author seunggu.lee
 */
public interface UploadFileUseCase {

    /**
     * 파일을 업로드한다.
     *
     * @param command 업로드 명령
     * @return 업로드 결과
     */
    FileUploadResult uploadFile(FileUploadCommand command);

    /**
     * 파일 업로드 명령.
     *
     * @param userId 사용자 ID
     * @param inputStream 파일 입력 스트림
     * @param originalFileName 원본 파일명
     * @param contentType 파일 MIME 타입
     * @param fileSize 파일 크기
     */
    record FileUploadCommand(
            Long userId,
            InputStream inputStream,
            String originalFileName,
            String contentType,
            long fileSize
    ) {}

    /**
     * 파일 업로드 결과.
     *
     * @param objectId 업로드된 객체의 불투명 식별자(저장 객체 키). 파일 메시지 전송 시 이 값을 보내면
     *                 서버가 소유·존재를 검증하고 URL/메타를 재구성한다(클라이언트 URL 위조 방지).
     * @param fileUrl 업로드된 파일 URL(하위호환용. 신규 클라이언트는 {@code objectId}만 사용 권장)
     * @param fileName 저장된 파일명
     * @param contentType 파일 MIME 타입
     * @param fileSize 파일 크기
     */
    record FileUploadResult(
            String objectId,
            String fileUrl,
            String fileName,
            String contentType,
            long fileSize
    ) {}
}
