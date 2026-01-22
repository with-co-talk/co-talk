package com.cotalk.adapter.inbound.websocket.dto;

import java.time.LocalDateTime;

/**
 * WebSocket으로 전송할 메시지 DTO.
 * <p>
 * 인메모리 브로커에서 ChatBroadcastMessage를 변환하여 WebSocket으로 전송할 때 사용됩니다.
 *
 * @param messageId    메시지 ID
 * @param senderId     발신자 ID
 * @param roomId       채팅방 ID
 * @param content      메시지 내용
 * @param type         메시지 타입
 * @param createdAt    생성 일시
 * @param fileUrl      파일 URL (파일 메시지인 경우)
 * @param fileName     파일명 (파일 메시지인 경우)
 * @param fileSize     파일 크기 (파일 메시지인 경우)
 * @param contentType  컨텐츠 타입 (파일 메시지인 경우)
 * @param thumbnailUrl 썸네일 URL (이미지 메시지인 경우)
 * @param unreadCount  읽지 않은 멤버 수 (발신자 제외)
 * @author seunggu.lee
 */
public record WebSocketMessage(
        Long messageId,
        Long senderId,
        Long roomId,
        String content,
        String type,
        LocalDateTime createdAt,
        String fileUrl,
        String fileName,
        Long fileSize,
        String contentType,
        String thumbnailUrl,
        Integer unreadCount
) {}
