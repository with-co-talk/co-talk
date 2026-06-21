package com.cotalk.adapter.outbound.persistence.mapper;

import com.cotalk.adapter.outbound.persistence.entity.ChatRoomJpaEntity;
import com.cotalk.domain.entity.ChatRoom;
import org.springframework.stereotype.Component;

/**
 * ChatRoom 도메인과 ChatRoomJpaEntity 간 매핑.
 *
 * @author seunggu.lee
 */
@Component
public class ChatRoomMapper {

    /**
     * JPA 엔티티를 도메인 엔티티로 변환한다.
     *
     * @param jpa JPA 엔티티
     * @return 도메인 엔티티, jpa가 null이면 null
     */
    public ChatRoom toDomain(ChatRoomJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }
        return ChatRoom.builder()
                .id(jpa.getId())
                .name(jpa.getName())
                .announcement(jpa.getAnnouncement())
                .imageUrl(jpa.getImageUrl())
                .type(jpa.getType())
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
    public ChatRoomJpaEntity toJpa(ChatRoom domain) {
        if (domain == null) {
            return null;
        }
        ChatRoomJpaEntity jpa = ChatRoomJpaEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .announcement(domain.getAnnouncement())
                .imageUrl(domain.getImageUrl())
                .type(domain.getType())
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
