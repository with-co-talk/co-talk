package com.cotalk.domain.port.inbound;

import java.io.InputStream;

/**
 * 파일 업로드 유즈케이스
 */
public interface UploadFileUseCase {

    /**
     * 파일 업로드
     *
     * @param command 업로드 명령
     * @return 업로드 결과
     */
    FileUploadResult uploadFile(FileUploadCommand command);

    /**
     * 파일 업로드 명령
     */
    record FileUploadCommand(
            Long userId,
            InputStream inputStream,
            String originalFileName,
            String contentType,
            long fileSize
    ) {}

    /**
     * 파일 업로드 결과
     */
    record FileUploadResult(
            String fileUrl,
            String fileName,
            String contentType,
            long fileSize
    ) {}
}
