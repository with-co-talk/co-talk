package com.cotalk.adapter.inbound.websocket.dto;

/**
 * 파일 첨부 메시지 전송 요청 DTO.
 * WebSocket을 통해 클라이언트로부터 수신되는 파일 메시지 요청입니다.
 *
 * @param senderId     발신자 사용자 ID
 * @param roomId       채팅방 ID
 * @param fileUrl      업로드된 파일의 URL
 * @param fileName     파일명
 * @param fileSize     파일 크기 (바이트)
 * @param contentType  파일의 MIME 타입
 * @param thumbnailUrl 썸네일 이미지 URL (이미지/동영상 파일인 경우)
 * @author seunggu.lee
 */
public record FileMessageRequest(
        Long senderId,
        Long roomId,
        String fileUrl,
        String fileName,
        Long fileSize,
        String contentType,
        String thumbnailUrl
) {}
