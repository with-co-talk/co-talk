package com.cotalk.adapter.inbound.rest.dto.auth;

/**
 * 파일 업로드 응답 DTO.
 *
 * @param objectId    업로드된 객체의 불투명 식별자(저장 객체 키). 파일 메시지 전송 시 이 값을 보내면
 *                    서버가 소유·존재를 검증하고 URL/메타를 재구성한다(권장).
 * @param fileUrl     업로드된 파일 URL (하위호환용)
 * @param fileName    파일명
 * @param contentType 콘텐츠 타입
 * @param fileSize    파일 크기 (바이트)
 * @param isImage     이미지 여부
 * @author seunggu.lee
 */
public record FileUploadResponse(
        String objectId,
        String fileUrl,
        String fileName,
        String contentType,
        long fileSize,
        boolean isImage
) {

    /**
     * FileUploadResponse를 생성한다.
     *
     * @param objectId    업로드된 객체의 불투명 식별자(저장 객체 키)
     * @param fileUrl     업로드된 파일 URL
     * @param fileName    파일명
     * @param contentType 콘텐츠 타입
     * @param fileSize    파일 크기 (바이트)
     * @param isImage     이미지 여부
     * @return FileUploadResponse 인스턴스
     */
    public static FileUploadResponse of(String objectId, String fileUrl, String fileName,
                                        String contentType, long fileSize, boolean isImage) {
        return new FileUploadResponse(objectId, fileUrl, fileName, contentType, fileSize, isImage);
    }
}
