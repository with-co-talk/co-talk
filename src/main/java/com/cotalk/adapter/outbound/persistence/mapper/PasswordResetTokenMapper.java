package com.cotalk.adapter.outbound.persistence.mapper;

import com.cotalk.adapter.outbound.persistence.entity.PasswordResetTokenJpaEntity;
import com.cotalk.domain.entity.PasswordResetToken;
import org.springframework.stereotype.Component;

/**
 * PasswordResetToken 도메인과 PasswordResetTokenJpaEntity 간 매핑.
 *
 * @author seunggu.lee
 */
@Component
public class PasswordResetTokenMapper {

    /**
     * JPA 엔티티를 도메인 엔티티로 변환한다.
     *
     * @param jpa JPA 엔티티
     * @return 도메인 엔티티, jpa가 null이면 null
     */
    public PasswordResetToken toDomain(PasswordResetTokenJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }
        return PasswordResetToken.builder()
                .id(jpa.getId())
                .token(jpa.getToken())
                .userId(jpa.getUserId())
                .email(jpa.getEmail())
                .expiresAt(jpa.getExpiresAt())
                .usedAt(jpa.getUsedAt())
                .verificationCode(jpa.getVerificationCode())
                .failedAttempts(jpa.getFailedAttempts())
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
    public PasswordResetTokenJpaEntity toJpa(PasswordResetToken domain) {
        if (domain == null) {
            return null;
        }
        PasswordResetTokenJpaEntity jpa = PasswordResetTokenJpaEntity.builder()
                .id(domain.getId())
                .token(domain.getToken())
                .userId(domain.getUserId())
                .email(domain.getEmail())
                .expiresAt(domain.getExpiresAt())
                .usedAt(domain.getUsedAt())
                .verificationCode(domain.getVerificationCode())
                .failedAttempts(domain.getFailedAttempts())
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
