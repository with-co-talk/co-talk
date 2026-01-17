package com.cotalk.adapter.inbound.rest.dto.auth;

/**
 * 파일 업로드 응답 DTO.
 *
 * @param fileUrl     업로드된 파일 URL
 * @param fileName    파일명
 * @param contentType 콘텐츠 타입
 * @param fileSize    파일 크기 (바이트)
 * @param isImage     이미지 여부
 * @author seunggu.lee
 */
public record FileUploadResponse(
        String fileUrl,
        String fileName,
        String contentType,
        long fileSize,
        boolean isImage
) {

    /**
     * FileUploadResponse를 생성한다.
     *
     * @param fileUrl     업로드된 파일 URL
     * @param fileName    파일명
     * @param contentType 콘텐츠 타입
     * @param fileSize    파일 크기 (바이트)
     * @param isImage     이미지 여부
     * @return FileUploadResponse 인스턴스
     */
    public static FileUploadResponse of(String fileUrl, String fileName, String contentType, long fileSize, boolean isImage) {
        return new FileUploadResponse(fileUrl, fileName, contentType, fileSize, isImage);
    }
}
