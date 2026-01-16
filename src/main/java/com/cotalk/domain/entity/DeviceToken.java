package com.cotalk.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자의 푸시 알림용 디바이스 토큰 엔티티.
 * FCM (Android/Web) 및 APNs (iOS) 토큰을 저장합니다.
 */
@Entity
@Table(name = "device_tokens",
        indexes = {
                @Index(name = "idx_device_token_user_id", columnList = "user_id"),
                @Index(name = "idx_device_token_token", columnList = "token", unique = true)
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceToken {

    @Id
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceType deviceType;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public DeviceToken(Long id, Long userId, String token, DeviceType deviceType) {
        this.id = id;
        this.userId = userId;
        this.token = token;
        this.deviceType = deviceType;
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 토큰을 비활성화합니다.
     * FCM에서 토큰이 만료되었거나 사용자가 로그아웃할 때 호출됩니다.
     */
    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 토큰을 업데이트하고 활성화합니다.
     */
    public void updateToken(String newToken) {
        this.token = newToken;
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 디바이스 타입
     */
    public enum DeviceType {
        ANDROID,
        IOS,
        WEB
    }
}
