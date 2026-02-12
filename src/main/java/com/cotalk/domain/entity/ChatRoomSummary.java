package com.cotalk.domain.entity;

import java.time.LocalDateTime;

/**
 * 채팅방 요약 정보 레코드.
 * 채팅방 목록 조회 시 필요한 요약 정보를 나타낸다.
 *
 * @param id 채팅방 ID
 * @param name 채팅방 이름
 * @param imageUrl 채팅방 이미지 URL (그룹 채팅방인 경우)
 * @param type 채팅방 유형
 * @param createdAt 생성 시간
 * @param lastMessage 마지막 메시지 내용
 * @param lastMessageType 마지막 메시지 유형 (TEXT, IMAGE, FILE)
 * @param lastMessageAt 마지막 메시지 시간
 * @param unreadCount 읽지 않은 메시지 수
 * @param otherUserId 상대방 사용자 ID (1:1 채팅방인 경우)
 * @param otherUserNickname 상대방 닉네임 (1:1 채팅방인 경우)
 * @param otherUserAvatarUrl 상대방 프로필 이미지 URL (1:1 채팅방인 경우)
 * @param isOtherUserLeft 상대방이 채팅방을 나갔는지 여부 (1:1 채팅방인 경우)
 * @param isOtherUserOnline 상대방의 온라인 여부 (1:1 채팅방인 경우)
 * @param otherUserLastActiveAt 상대방의 마지막 접속 시간 (1:1 채팅방인 경우)
 * @author seunggu.lee
 */
public record ChatRoomSummary(
        Long id,
        String name,
        String imageUrl,
        ChatRoom.ChatRoomType type,
        LocalDateTime createdAt,
        String lastMessage,
        String lastMessageType,
        LocalDateTime lastMessageAt,
        long unreadCount,
        Long otherUserId,
        String otherUserNickname,
        String otherUserAvatarUrl,
        boolean isOtherUserLeft,
        boolean isOtherUserOnline,
        LocalDateTime otherUserLastActiveAt
) {}
