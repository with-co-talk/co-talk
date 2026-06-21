package com.cotalk.adapter.outbound.persistence.mapper;

import com.cotalk.adapter.outbound.persistence.entity.FriendJpaEntity;
import com.cotalk.domain.entity.Friend;
import org.springframework.stereotype.Component;

/**
 * Friend 도메인과 FriendJpaEntity 간 매핑.
 *
 * @author seunggu.lee
 */
@Component
public class FriendMapper {

    /**
     * JPA 엔티티를 도메인 엔티티로 변환한다.
     *
     * @param jpa JPA 엔티티
     * @return 도메인 엔티티, jpa가 null이면 null
     */
    public Friend toDomain(FriendJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }
        return Friend.builder()
                .id(jpa.getId())
                .userId(jpa.getUserId())
                .friendId(jpa.getFriendId())
                .status(jpa.getStatus())
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
    public FriendJpaEntity toJpa(Friend domain) {
        if (domain == null) {
            return null;
        }
        FriendJpaEntity jpa = FriendJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .friendId(domain.getFriendId())
                .status(domain.getStatus())
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
