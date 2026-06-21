package com.cotalk.adapter.outbound.persistence.mapper;

import com.cotalk.adapter.outbound.persistence.entity.TermsAgreementJpaEntity;
import com.cotalk.domain.entity.TermsAgreement;
import org.springframework.stereotype.Component;

/**
 * TermsAgreement 도메인과 TermsAgreementJpaEntity 간 매핑.
 *
 * @author seunggu.lee
 */
@Component
public class TermsAgreementMapper {

    /**
     * JPA 엔티티를 도메인 엔티티로 변환한다.
     *
     * @param jpa JPA 엔티티
     * @return 도메인 엔티티, jpa가 null이면 null
     */
    public TermsAgreement toDomain(TermsAgreementJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }
        return TermsAgreement.builder()
                .id(jpa.getId())
                .userId(jpa.getUserId())
                .termsType(jpa.getTermsType())
                .termsVersion(jpa.getTermsVersion())
                .agreed(jpa.isAgreed())
                .agreedAt(jpa.getAgreedAt())
                .withdrawnAt(jpa.getWithdrawnAt())
                .ipAddress(jpa.getIpAddress())
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
    public TermsAgreementJpaEntity toJpa(TermsAgreement domain) {
        if (domain == null) {
            return null;
        }
        TermsAgreementJpaEntity jpa = TermsAgreementJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .termsType(domain.getTermsType())
                .termsVersion(domain.getTermsVersion())
                .agreed(domain.isAgreed())
                .agreedAt(domain.getAgreedAt())
                .withdrawnAt(domain.getWithdrawnAt())
                .ipAddress(domain.getIpAddress())
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
