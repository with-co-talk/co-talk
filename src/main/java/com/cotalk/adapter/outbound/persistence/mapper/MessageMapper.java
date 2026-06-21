package com.cotalk.adapter.outbound.persistence.mapper;

import com.cotalk.adapter.outbound.persistence.entity.MessageJpaEntity;
import com.cotalk.domain.entity.Message;
import org.springframework.stereotype.Component;

/**
 * Message 도메인과 MessageJpaEntity 간 매핑.
 *
 * @author seunggu.lee
 */
@Component
public class MessageMapper {

    /**
     * JPA 엔티티를 도메인 엔티티로 변환한다.
     *
     * @param jpa JPA 엔티티 (content는 컨버터로 복호화된 평문)
     * @return 도메인 엔티티, jpa가 null이면 null
     */
    public Message toDomain(MessageJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }
        return Message.builder()
                .id(jpa.getId())
                .chatRoomId(jpa.getChatRoomId())
                .senderId(jpa.getSenderId())
                .content(jpa.getContent())
                .type(jpa.getType())
                .fileUrl(jpa.getFileUrl())
                .fileName(jpa.getFileName())
                .fileSize(jpa.getFileSize())
                .fileContentType(jpa.getFileContentType())
                .thumbnailUrl(jpa.getThumbnailUrl())
                .replyToMessageId(jpa.getReplyToMessageId())
                .forwardedFromMessageId(jpa.getForwardedFromMessageId())
                .linkPreviewUrl(jpa.getLinkPreviewUrl())
                .linkPreviewTitle(jpa.getLinkPreviewTitle())
                .linkPreviewDescription(jpa.getLinkPreviewDescription())
                .linkPreviewImageUrl(jpa.getLinkPreviewImageUrl())
                .deletedAt(jpa.getDeletedAt())
                .deleted(jpa.isDeleted())
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .build();
    }

    /**
     * 도메인 엔티티를 JPA 엔티티로 변환한다.
     *
     * @param domain 도메인 엔티티 (content 평문; 저장 시 컨버터가 암호화)
     * @return JPA 엔티티, domain이 null이면 null
     */
    public MessageJpaEntity toJpa(Message domain) {
        if (domain == null) {
            return null;
        }
        MessageJpaEntity jpa = MessageJpaEntity.builder()
                .id(domain.getId())
                .chatRoomId(domain.getChatRoomId())
                .senderId(domain.getSenderId())
                .content(domain.getContent())
                .type(domain.getType())
                .fileUrl(domain.getFileUrl())
                .fileName(domain.getFileName())
                .fileSize(domain.getFileSize())
                .fileContentType(domain.getFileContentType())
                .thumbnailUrl(domain.getThumbnailUrl())
                .replyToMessageId(domain.getReplyToMessageId())
                .forwardedFromMessageId(domain.getForwardedFromMessageId())
                .linkPreviewUrl(domain.getLinkPreviewUrl())
                .linkPreviewTitle(domain.getLinkPreviewTitle())
                .linkPreviewDescription(domain.getLinkPreviewDescription())
                .linkPreviewImageUrl(domain.getLinkPreviewImageUrl())
                .deletedAt(domain.getDeletedAt())
                .deleted(domain.isDeleted())
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
