package com.cotalk.adapter.outbound.persistence.entity;

import com.cotalk.domain.entity.DeviceToken;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 디바이스 토큰 JPA 엔티티.
 * persistence 계층 전용이며, 도메인 DeviceToken과 매핑된다.
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
public class DeviceTokenJpaEntity extends BaseJpaEntity {

    @Id
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceToken.DeviceType deviceType;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
