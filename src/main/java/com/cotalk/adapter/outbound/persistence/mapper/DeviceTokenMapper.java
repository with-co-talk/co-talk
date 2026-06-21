package com.cotalk.adapter.outbound.persistence.mapper;

import com.cotalk.adapter.outbound.persistence.entity.DeviceTokenJpaEntity;
import com.cotalk.domain.entity.DeviceToken;
import org.springframework.stereotype.Component;

/**
 * DeviceToken 도메인과 DeviceTokenJpaEntity 간 매핑.
 *
 * @author seunggu.lee
 */
@Component
public class DeviceTokenMapper {

    /**
     * JPA 엔티티를 도메인 엔티티로 변환한다.
     *
     * @param jpa JPA 엔티티
     * @return 도메인 엔티티, jpa가 null이면 null
     */
    public DeviceToken toDomain(DeviceTokenJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }
        return DeviceToken.builder()
                .id(jpa.getId())
                .userId(jpa.getUserId())
                .token(jpa.getToken())
                .deviceType(jpa.getDeviceType())
                .active(jpa.isActive())
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .build();
    }

    /**
     * 도메인 엔티티를 JPA 엔티티로 변환한다.
     *
     * @param domain 도메인 엔티티
     * @return JPA 엔티티, domain이 null이면 null
     */
    public DeviceTokenJpaEntity toJpa(DeviceToken domain) {
        if (domain == null) {
            return null;
        }
        DeviceTokenJpaEntity jpa = DeviceTokenJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .token(domain.getToken())
                .deviceType(domain.getDeviceType())
                .active(domain.isActive())
                .build();
        if (domain.getCreatedAt() != null) {
            jpa.setCreatedAt(domain.getCreatedAt());
        }
        if (domain.getUpdatedAt() != null) {
            jpa.setUpdatedAt(domain.getUpdatedAt());
        }
        return jpa;
    }
}
