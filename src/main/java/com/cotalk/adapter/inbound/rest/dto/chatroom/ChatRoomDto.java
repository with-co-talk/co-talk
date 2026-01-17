package com.cotalk.adapter.inbound.rest.dto.chatroom;

import com.cotalk.domain.entity.ChatRoomSummary;

import java.time.LocalDateTime;

/**
 * 채팅방 정보 DTO.
 *
 * @param id                 채팅방 ID
 * @param name               채팅방 이름
 * @param type               채팅방 타입 (PRIVATE, GROUP)
 * @param createdAt          생성 일시
 * @param lastMessage        마지막 메시지
 * @param lastMessageAt      마지막 메시지 일시
 * @param unreadCount        읽지 않은 메시지 수
 * @param otherUserId        상대방 사용자 ID (1:1 채팅방인 경우)
 * @param otherUserNickname  상대방 닉네임 (1:1 채팅방인 경우)
 * @param otherUserAvatarUrl 상대방 아바타 URL (1:1 채팅방인 경우)
 * @author seunggu.lee
 */
public record ChatRoomDto(
        Long id,
        String name,
        String type,
        LocalDateTime createdAt,
        String lastMessage,
        LocalDateTime lastMessageAt,
        long unreadCount,
        Long otherUserId,
        String otherUserNickname,
        String otherUserAvatarUrl
) {

    /**
     * ChatRoomSummary 엔티티로부터 DTO를 생성합니다.
     *
     * @param summary ChatRoomSummary 엔티티
     * @return ChatRoomDto 인스턴스
     */
    public static ChatRoomDto from(ChatRoomSummary summary) {
        return new ChatRoomDto(
                summary.id(),
                summary.name(),
                summary.type().name(),
                summary.createdAt(),
                summary.lastMessage(),
                summary.lastMessageAt(),
                summary.unreadCount(),
                summary.otherUserId(),
                summary.otherUserNickname(),
                summary.otherUserAvatarUrl()
        );
    }
}
