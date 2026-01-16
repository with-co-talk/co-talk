package com.cotalk.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 메시지 반응(이모지) 엔티티
 */
@Entity
@Table(name = "message_reactions", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "user_id", "emoji"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MessageReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String emoji;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static MessageReaction create(Long messageId, Long userId, String emoji) {
        return MessageReaction.builder()
                .messageId(messageId)
                .userId(userId)
                .emoji(emoji)
                .build();
    }

    public boolean isFromUser(Long userId) {
        return this.userId.equals(userId);
    }
}
