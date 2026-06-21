package com.cotalk.adapter.outbound.persistence.mapper;

import com.cotalk.adapter.outbound.persistence.entity.ProfileHistoryJpaEntity;
import com.cotalk.domain.entity.ProfileHistory;
import org.springframework.stereotype.Component;

/**
 * ProfileHistory 도메인과 ProfileHistoryJpaEntity 간 매핑.
 *
 * @author seunggu.lee
 */
@Component
public class ProfileHistoryMapper {

    /**
     * JPA 엔티티를 도메인 엔티티로 변환한다.
     *
     * @param jpa JPA 엔티티
     * @return 도메인 엔티티, jpa가 null이면 null
     */
    public ProfileHistory toDomain(ProfileHistoryJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }
        return ProfileHistory.builder()
                .id(jpa.getId())
                .userId(jpa.getUserId())
                .type(jpa.getType())
                .url(jpa.getUrl())
                .content(jpa.getContent())
                .isPrivate(jpa.isPrivate())
                .isCurrent(jpa.isCurrent())
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
    public ProfileHistoryJpaEntity toJpa(ProfileHistory domain) {
        if (domain == null) {
            return null;
        }
        ProfileHistoryJpaEntity jpa = ProfileHistoryJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .type(domain.getType())
                .url(domain.getUrl())
                .content(domain.getContent())
                .isPrivate(domain.isPrivate())
                .isCurrent(domain.isCurrent())
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
