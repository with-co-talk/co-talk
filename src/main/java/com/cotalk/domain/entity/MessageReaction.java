package com.cotalk.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 메시지 반응 엔티티.
 * 메시지에 대한 이모지 반응 정보를 나타낸다.
 *
 * @author seunggu.lee
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

    /**
     * 엔티티 생성 시 호출되는 콜백 메서드.
     * 생성 시간을 현재 시간으로 설정한다.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * 메시지 반응을 생성한다.
     *
     * @param messageId 메시지 ID
     * @param userId 사용자 ID
     * @param emoji 이모지 문자열
     * @return 생성된 MessageReaction 인스턴스
     */
    public static MessageReaction create(Long messageId, Long userId, String emoji) {
        return MessageReaction.builder()
                .messageId(messageId)
                .userId(userId)
                .emoji(emoji)
                .build();
    }

    /**
     * 지정된 사용자가 남긴 반응인지 확인한다.
     *
     * @param userId 확인할 사용자 ID
     * @return 해당 사용자의 반응이면 true, 그렇지 않으면 false
     */
    public boolean isFromUser(Long userId) {
        return this.userId.equals(userId);
    }
}
