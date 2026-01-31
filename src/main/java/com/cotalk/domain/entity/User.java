package com.cotalk.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자 엔티티.
 * 시스템의 사용자 정보를 나타낸다.
 *
 * @author seunggu.lee
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false)
    private String nickname;

    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider")
    private OAuthProvider oauthProvider;

    @Column(name = "oauth_id")
    private String oauthId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "online_status", nullable = false)
    @Builder.Default
    private OnlineStatus onlineStatus = OnlineStatus.OFFLINE;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "status_message", length = 60)
    private String statusMessage;

    @Column(name = "background_url", length = 500)
    private String backgroundUrl;

    /**
     * 사용자 상태를 나타내는 열거형.
     *
     * @author seunggu.lee
     */
    public enum UserStatus {
        /** 활성 상태 */
        ACTIVE,
        /** 비활성 상태 */
        INACTIVE,
        /** 정지 상태 */
        SUSPENDED
    }

    /**
     * 사용자 온라인 상태를 나타내는 열거형.
     *
     * @author seunggu.lee
     */
    public enum OnlineStatus {
        /** 온라인 상태 */
        ONLINE,
        /** 오프라인 상태 */
        OFFLINE,
        /** 자리비움 상태 */
        AWAY
    }

    /**
     * OAuth 제공자를 나타내는 열거형.
     *
     * @author seunggu.lee
     */
    public enum OAuthProvider {
        /** 카카오 */
        KAKAO,
        /** 구글 */
        GOOGLE,
        /** 애플 */
        APPLE
    }

    /**
     * 사용자 역할을 나타내는 열거형.
     *
     * @author seunggu.lee
     */
    public enum Role {
        /** 일반 사용자 */
        USER,
        /** 관리자 */
        ADMIN
    }

    /**
     * 닉네임을 변경한다.
     *
     * @param nickname 새 닉네임
     * @throws IllegalArgumentException 닉네임이 null이거나 비어있는 경우
     */
    public void updateNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임은 비어있을 수 없습니다.");
        }
        this.nickname = nickname;
    }

    /**
     * 프로필 이미지 URL을 변경한다.
     *
     * @param avatarUrl 새 프로필 이미지 URL
     */
    public void updateAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    /**
     * 비밀번호를 변경한다.
     *
     * @param newPasswordHash 새 비밀번호 해시
     * @throws IllegalArgumentException 비밀번호 해시가 null이거나 비어있는 경우
     */
    public void updatePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 비어있을 수 없습니다.");
        }
        this.passwordHash = newPasswordHash;
    }

    /**
     * 계정을 비활성화한다.
     */
    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }

    /**
     * 계정을 정지시킨다.
     */
    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    /**
     * 계정을 활성화한다.
     */
    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    /**
     * 사용자가 활성 상태인지 확인한다.
     *
     * @return 활성 상태이면 true, 그렇지 않으면 false
     */
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    /**
     * 사용자를 온라인 상태로 설정한다.
     * 마지막 활동 시간도 함께 갱신한다.
     */
    public void setOnline() {
        this.onlineStatus = OnlineStatus.ONLINE;
        this.lastActiveAt = LocalDateTime.now();
    }

    /**
     * 사용자를 오프라인 상태로 설정한다.
     * 마지막 활동 시간도 함께 갱신한다.
     */
    public void setOffline() {
        this.onlineStatus = OnlineStatus.OFFLINE;
        this.lastActiveAt = LocalDateTime.now();
    }

    /**
     * 사용자를 자리비움 상태로 설정한다.
     * 마지막 활동 시간도 함께 갱신한다.
     */
    public void setAway() {
        this.onlineStatus = OnlineStatus.AWAY;
        this.lastActiveAt = LocalDateTime.now();
    }

    /**
     * 마지막 활동 시간을 현재 시간으로 갱신한다.
     */
    public void updateLastActiveAt() {
        this.lastActiveAt = LocalDateTime.now();
    }

    /**
     * 사용자가 온라인 상태인지 확인한다.
     *
     * @return 온라인 상태이면 true, 그렇지 않으면 false
     */
    public boolean isOnline() {
        return this.onlineStatus == OnlineStatus.ONLINE;
    }

    /**
     * OAuth 사용자인지 확인한다.
     *
     * @return OAuth 사용자이면 true, 그렇지 않으면 false
     */
    public boolean isOAuthUser() {
        return this.oauthProvider != null;
    }

    /**
     * 상태메시지를 변경한다.
     *
     * @param statusMessage 새 상태메시지 (null 허용, 최대 60자)
     */
    public void updateStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    /**
     * 배경화면 URL을 변경한다.
     *
     * @param backgroundUrl 새 배경화면 URL
     */
    public void updateBackgroundUrl(String backgroundUrl) {
        this.backgroundUrl = backgroundUrl;
    }
}
