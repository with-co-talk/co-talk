package com.cotalk.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String nickname;

    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "online_status", nullable = false)
    @Builder.Default
    private OnlineStatus onlineStatus = OnlineStatus.OFFLINE;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    public enum UserStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }

    public enum OnlineStatus {
        ONLINE, OFFLINE, AWAY
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

    public void updateNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임은 비어있을 수 없습니다.");
        }
        this.nickname = nickname;
    }

    public void updateAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void updatePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 비어있을 수 없습니다.");
        }
        this.passwordHash = newPasswordHash;
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public void setOnline() {
        this.onlineStatus = OnlineStatus.ONLINE;
        this.lastActiveAt = LocalDateTime.now();
    }

    public void setOffline() {
        this.onlineStatus = OnlineStatus.OFFLINE;
        this.lastActiveAt = LocalDateTime.now();
    }

    public void setAway() {
        this.onlineStatus = OnlineStatus.AWAY;
        this.lastActiveAt = LocalDateTime.now();
    }

    public void updateLastActiveAt() {
        this.lastActiveAt = LocalDateTime.now();
    }

    public boolean isOnline() {
        return this.onlineStatus == OnlineStatus.ONLINE;
    }
}
