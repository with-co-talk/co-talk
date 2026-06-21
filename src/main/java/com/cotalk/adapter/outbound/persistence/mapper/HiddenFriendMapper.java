package com.cotalk.adapter.outbound.persistence.mapper;

import com.cotalk.adapter.outbound.persistence.entity.HiddenFriendJpaEntity;
import com.cotalk.domain.entity.HiddenFriend;
import org.springframework.stereotype.Component;

/**
 * HiddenFriend 도메인과 HiddenFriendJpaEntity 간 매핑.
 *
 * @author seunggu.lee
 */
@Component
public class HiddenFriendMapper {

    /**
     * JPA 엔티티를 도메인 엔티티로 변환한다.
     *
     * @param jpa JPA 엔티티
     * @return 도메인 엔티티, jpa가 null이면 null
     */
    public HiddenFriend toDomain(HiddenFriendJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }
        return HiddenFriend.builder()
                .id(jpa.getId())
                .userId(jpa.getUserId())
                .friendId(jpa.getFriendId())
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
    public HiddenFriendJpaEntity toJpa(HiddenFriend domain) {
        if (domain == null) {
            return null;
        }
        HiddenFriendJpaEntity jpa = HiddenFriendJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .friendId(domain.getFriendId())
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
