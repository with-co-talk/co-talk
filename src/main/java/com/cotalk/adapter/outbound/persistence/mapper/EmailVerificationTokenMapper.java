package com.cotalk.adapter.outbound.persistence.mapper;

import com.cotalk.adapter.outbound.persistence.entity.EmailVerificationTokenJpaEntity;
import com.cotalk.domain.entity.EmailVerificationToken;
import org.springframework.stereotype.Component;

/**
 * EmailVerificationToken 도메인과 EmailVerificationTokenJpaEntity 간 매핑.
 *
 * @author seunggu.lee
 */
@Component
public class EmailVerificationTokenMapper {

    /**
     * JPA 엔티티를 도메인 엔티티로 변환한다.
     *
     * @param jpa JPA 엔티티
     * @return 도메인 엔티티, jpa가 null이면 null
     */
    public EmailVerificationToken toDomain(EmailVerificationTokenJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }
        return EmailVerificationToken.builder()
                .id(jpa.getId())
                .token(jpa.getToken())
                .userId(jpa.getUserId())
                .email(jpa.getEmail())
                .expiresAt(jpa.getExpiresAt())
                .verifiedAt(jpa.getVerifiedAt())
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
    public EmailVerificationTokenJpaEntity toJpa(EmailVerificationToken domain) {
        if (domain == null) {
            return null;
        }
        EmailVerificationTokenJpaEntity jpa = EmailVerificationTokenJpaEntity.builder()
                .id(domain.getId())
                .token(domain.getToken())
                .userId(domain.getUserId())
                .email(domain.getEmail())
                .expiresAt(domain.getExpiresAt())
                .verifiedAt(domain.getVerifiedAt())
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
