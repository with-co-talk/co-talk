package com.cotalk.adapter.outbound.persistence.mapper;

import com.cotalk.adapter.outbound.persistence.entity.FriendRequestJpaEntity;
import com.cotalk.domain.entity.FriendRequest;
import org.springframework.stereotype.Component;

/**
 * FriendRequest 도메인과 FriendRequestJpaEntity 간 매핑.
 *
 * @author seunggu.lee
 */
@Component
public class FriendRequestMapper {

    /**
     * JPA 엔티티를 도메인 엔티티로 변환한다.
     *
     * @param jpa JPA 엔티티
     * @return 도메인 엔티티, jpa가 null이면 null
     */
    public FriendRequest toDomain(FriendRequestJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }
        return FriendRequest.builder()
                .id(jpa.getId())
                .requesterId(jpa.getRequesterId())
                .receiverId(jpa.getReceiverId())
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
    public FriendRequestJpaEntity toJpa(FriendRequest domain) {
        if (domain == null) {
            return null;
        }
        FriendRequestJpaEntity jpa = FriendRequestJpaEntity.builder()
                .id(domain.getId())
                .requesterId(domain.getRequesterId())
                .receiverId(domain.getReceiverId())
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
