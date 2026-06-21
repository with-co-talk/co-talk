package com.cotalk.adapter.outbound.persistence.mapper;

import com.cotalk.adapter.outbound.persistence.entity.MessageReactionJpaEntity;
import com.cotalk.domain.entity.MessageReaction;
import org.springframework.stereotype.Component;

/**
 * MessageReaction 도메인과 MessageReactionJpaEntity 간 매핑.
 *
 * @author seunggu.lee
 */
@Component
public class MessageReactionMapper {

    /**
     * JPA 엔티티를 도메인 엔티티로 변환한다.
     *
     * @param jpa JPA 엔티티
     * @return 도메인 엔티티, jpa가 null이면 null
     */
    public MessageReaction toDomain(MessageReactionJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }
        return MessageReaction.builder()
                .id(jpa.getId())
                .messageId(jpa.getMessageId())
                .userId(jpa.getUserId())
                .emoji(jpa.getEmoji())
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
    public MessageReactionJpaEntity toJpa(MessageReaction domain) {
        if (domain == null) {
            return null;
        }
        MessageReactionJpaEntity jpa = MessageReactionJpaEntity.builder()
                .id(domain.getId())
                .messageId(domain.getMessageId())
                .userId(domain.getUserId())
                .emoji(domain.getEmoji())
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
