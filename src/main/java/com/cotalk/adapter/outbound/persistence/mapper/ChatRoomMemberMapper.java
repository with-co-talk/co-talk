package com.cotalk.adapter.outbound.persistence.mapper;

import com.cotalk.adapter.outbound.persistence.entity.ChatRoomMemberJpaEntity;
import com.cotalk.domain.entity.ChatRoomMember;
import org.springframework.stereotype.Component;

/**
 * ChatRoomMember 도메인과 ChatRoomMemberJpaEntity 간 매핑.
 *
 * @author seunggu.lee
 */
@Component
public class ChatRoomMemberMapper {

    /**
     * JPA 엔티티를 도메인 엔티티로 변환한다.
     *
     * @param jpa JPA 엔티티
     * @return 도메인 엔티티, jpa가 null이면 null
     */
    public ChatRoomMember toDomain(ChatRoomMemberJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }
        return ChatRoomMember.builder()
                .id(jpa.getId())
                .chatRoomId(jpa.getChatRoomId())
                .userId(jpa.getUserId())
                .role(jpa.getRole())
                .lastReadAt(jpa.getLastReadAt())
                .lastReadMessageId(jpa.getLastReadMessageId())
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
    public ChatRoomMemberJpaEntity toJpa(ChatRoomMember domain) {
        if (domain == null) {
            return null;
        }
        ChatRoomMemberJpaEntity jpa = ChatRoomMemberJpaEntity.builder()
                .id(domain.getId())
                .chatRoomId(domain.getChatRoomId())
                .userId(domain.getUserId())
                .role(domain.getRole())
                .lastReadAt(domain.getLastReadAt())
                .lastReadMessageId(domain.getLastReadMessageId())
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
