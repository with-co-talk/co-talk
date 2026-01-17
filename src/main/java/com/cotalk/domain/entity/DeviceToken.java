package com.cotalk.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 디바이스 토큰 엔티티.
 * 사용자의 푸시 알림용 디바이스 토큰 정보를 나타낸다.
 * FCM (Android/Web) 및 APNs (iOS) 토큰을 저장한다.
 *
 * @author seunggu.lee
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

    /**
     * DeviceToken 생성자.
     *
     * @param id 디바이스 토큰 ID
     * @param userId 사용자 ID
     * @param token 디바이스 토큰 문자열
     * @param deviceType 디바이스 유형
     */
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
     * 토큰을 비활성화한다.
     * FCM에서 토큰이 만료되었거나 사용자가 로그아웃할 때 호출된다.
     */
    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 토큰을 업데이트하고 활성화한다.
     *
     * @param newToken 새 토큰 문자열
     */
    public void updateToken(String newToken) {
        this.token = newToken;
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 디바이스 유형을 나타내는 열거형.
     *
     * @author seunggu.lee
     */
    public enum DeviceType {
        /** 안드로이드 디바이스 */
        ANDROID,
        /** iOS 디바이스 */
        IOS,
        /** 웹 브라우저 */
        WEB
    }
}
