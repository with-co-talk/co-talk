package com.cotalk.domain.entity;

import java.time.LocalDateTime;

public record ChatRoomSummary(
        Long id,
        String name,
        ChatRoom.ChatRoomType type,
        LocalDateTime createdAt,
        String lastMessage,
        LocalDateTime lastMessageAt,
        long unreadCount,
        Long otherUserId,
        String otherUserNickname,
        String otherUserAvatarUrl
) {}
