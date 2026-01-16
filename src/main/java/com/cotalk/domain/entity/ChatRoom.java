package com.cotalk.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomType type;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum ChatRoomType {
        DIRECT, GROUP
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 1:1 채팅방인지 확인합니다.
     */
    public boolean isDirectChat() {
        return type == ChatRoomType.DIRECT;
    }

    /**
     * 그룹 채팅방인지 확인합니다.
     */
    public boolean isGroupChat() {
        return type == ChatRoomType.GROUP;
    }

    /**
     * 그룹 채팅방의 경우 이름이 필수인지 확인합니다.
     */
    public boolean requiresName() {
        return isGroupChat();
    }

    /**
     * 채팅방 이름을 반환합니다. 1:1 채팅방의 경우 null을 반환할 수 있습니다.
     */
    public String getDisplayName() {
        return name;
    }

    /**
     * 채팅방 이름의 유효성을 검증합니다.
     */
    public boolean isValidName() {
        if (isDirectChat()) {
            return true; // 1:1 채팅방은 이름이 없어도 됨
        }
        return name != null && !name.trim().isEmpty();
    }
}
