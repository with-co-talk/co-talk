package com.cotalk.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "friends", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "friend_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Friend {

    @Id
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "friend_id", nullable = false)
    private Long friendId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum FriendStatus {
        PENDING, ACCEPTED, BLOCKED
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
     * 친구 관계가 수락 상태인지 확인합니다.
     */
    public boolean isAccepted() {
        return status == FriendStatus.ACCEPTED;
    }

    /**
     * 친구 관계가 차단 상태인지 확인합니다.
     */
    public boolean isBlocked() {
        return status == FriendStatus.BLOCKED;
    }

    /**
     * 친구 관계가 대기 상태인지 확인합니다.
     */
    public boolean isPending() {
        return status == FriendStatus.PENDING;
    }

    /**
     * 지정된 사용자가 이 친구 관계의 당사자인지 확인합니다.
     */
    public boolean involves(Long checkUserId) {
        return userId.equals(checkUserId) || friendId.equals(checkUserId);
    }

    /**
     * 지정된 사용자의 친구 ID를 반환합니다.
     */
    public Long getOtherUserId(Long currentUserId) {
        if (userId.equals(currentUserId)) {
            return friendId;
        } else if (friendId.equals(currentUserId)) {
            return userId;
        }
        throw new IllegalArgumentException("사용자가 이 친구 관계에 속하지 않습니다.");
    }
}
