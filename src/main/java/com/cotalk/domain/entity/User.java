package com.cotalk.domain.entity;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 사용자 도메인 엔티티.
 * 시스템의 사용자 정보를 나타낸다.
 * 순수 도메인 모델이며 JPA 어노테이션은 persistence 계층에만 존재한다.
 *
 * @author seunggu.lee
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class User extends DomainBaseEntity {

    private Long id;
    private String email;
    private String passwordHash;
    private String nickname;
    private String avatarUrl;
    private OAuthProvider oauthProvider;
    private String oauthId;
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;
    @Builder.Default
    private Role role = Role.USER;
    @Builder.Default
    private OnlineStatus onlineStatus = OnlineStatus.OFFLINE;
    @Builder.Default
    private boolean emailVerified = true;
    private LocalDateTime lastActiveAt;
    private String statusMessage;
    private String backgroundUrl;

    /** 사용자 상태 */
    public enum UserStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }

    /** 온라인 상태 */
    public enum OnlineStatus {
        ONLINE, OFFLINE, AWAY
    }

    /** OAuth 제공자 */
    public enum OAuthProvider {
        KAKAO, GOOGLE, APPLE
    }

    /** 사용자 역할 */
    public enum Role {
        USER, ADMIN
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

    public void activate() {
        this.status = UserStatus.ACTIVE;
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

    public boolean isOAuthUser() {
        return this.oauthProvider != null;
    }

    /**
     * 이메일 인증을 완료한다.
     */
    public void verifyEmail() {
        this.emailVerified = true;
    }

    /**
     * 이메일 인증 여부를 확인한다.
     *
     * @return 이메일 인증 완료 시 true
     */
    public boolean isEmailVerified() {
        return this.emailVerified;
    }

    public void updateStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public void updateBackgroundUrl(String backgroundUrl) {
        this.backgroundUrl = backgroundUrl;
    }
}
