package com.cotalk.adapter.inbound.rest.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 파일 메시지 전송 요청 DTO.
 *
 * @param chatRoomId   채팅방 ID
 * @param fileUrl      파일 URL
 * @param fileName     파일명
 * @param fileSize     파일 크기 (bytes)
 * @param contentType  파일 MIME 타입
 * @param thumbnailUrl 썸네일 URL (이미지인 경우)
 * @author seunggu.lee
 */
public record SendFileMessageRequest(
        @NotNull(message = "채팅방 ID는 필수입니다.")
        Long chatRoomId,

        @NotBlank(message = "파일 URL은 필수입니다.")
        String fileUrl,

        @NotBlank(message = "파일명은 필수입니다.")
        String fileName,

        @NotNull(message = "파일 크기는 필수입니다.")
        Long fileSize,

        @NotBlank(message = "파일 형식은 필수입니다.")
        String contentType,

        String thumbnailUrl  // 선택 (이미지인 경우)
) {

    /**
     * 파일 메시지 전송 요청을 생성합니다.
     *
     * @param chatRoomId   채팅방 ID
     * @param fileUrl      파일 URL
     * @param fileName     파일명
     * @param fileSize     파일 크기
     * @param contentType  파일 MIME 타입
     * @param thumbnailUrl 썸네일 URL
     * @return SendFileMessageRequest 인스턴스
     */
    public static SendFileMessageRequest of(Long chatRoomId, String fileUrl,
                                             String fileName, Long fileSize, String contentType,
                                             String thumbnailUrl) {
        return new SendFileMessageRequest(chatRoomId, fileUrl, fileName, fileSize, contentType, thumbnailUrl);
    }
}
