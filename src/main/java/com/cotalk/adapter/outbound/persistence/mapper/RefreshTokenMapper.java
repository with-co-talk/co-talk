package com.cotalk.adapter.outbound.persistence.mapper;

import com.cotalk.adapter.outbound.persistence.entity.RefreshTokenJpaEntity;
import com.cotalk.domain.entity.RefreshToken;
import org.springframework.stereotype.Component;

/**
 * RefreshToken 도메인과 RefreshTokenJpaEntity 간 매핑.
 *
 * @author seunggu.lee
 */
@Component
public class RefreshTokenMapper {

    /**
     * JPA 엔티티를 도메인 엔티티로 변환한다.
     *
     * @param jpa JPA 엔티티
     * @return 도메인 엔티티, jpa가 null이면 null
     */
    public RefreshToken toDomain(RefreshTokenJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }
        return RefreshToken.builder()
                .id(jpa.getId())
                .userId(jpa.getUserId())
                .token(jpa.getToken())
                .expiresAt(jpa.getExpiresAt())
                .revoked(jpa.isRevoked())
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
    public RefreshTokenJpaEntity toJpa(RefreshToken domain) {
        if (domain == null) {
            return null;
        }
        RefreshTokenJpaEntity jpa = RefreshTokenJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .token(domain.getToken())
                .expiresAt(domain.getExpiresAt())
                .revoked(domain.isRevoked())
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
