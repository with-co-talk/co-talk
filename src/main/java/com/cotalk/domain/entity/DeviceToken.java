package com.cotalk.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


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
@AllArgsConstructor
@Builder
public class DeviceToken extends BaseEntity {

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
    @Builder.Default
    private boolean active = true;

    /**
     * 토큰을 비활성화한다.
     * FCM에서 토큰이 만료되었거나 사용자가 로그아웃할 때 호출된다.
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * 토큰을 업데이트하고 활성화한다.
     *
     * @param newToken 새 토큰 문자열
     */
    public void updateToken(String newToken) {
        this.token = newToken;
        this.active = true;
    }

    /**
     * 토큰의 소유자를 이전하고 활성화한다.
     * 동일 토큰을 다른 사용자가 등록할 때, 기존 레코드를 삭제 후 재생성하는 대신
     * 소유자(userId)와 디바이스 타입을 갱신하여 UNIQUE 제약 충돌을 회피한다.
     *
     * @param newUserId    새 소유자 사용자 ID
     * @param newDeviceType 새 디바이스 타입
     */
    public void transferTo(Long newUserId, DeviceType newDeviceType) {
        this.userId = newUserId;
        this.deviceType = newDeviceType;
        this.active = true;
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
