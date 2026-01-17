package com.cotalk.adapter.inbound.rest.dto.message;

import com.cotalk.domain.entity.Message;

import java.time.LocalDateTime;

/**
 * 메시지 정보 DTO.
 *
 * @param id                     메시지 ID
 * @param senderId               발신자 ID
 * @param content                메시지 내용
 * @param type                   메시지 타입
 * @param createdAt              생성 일시
 * @param fileUrl                파일 URL
 * @param fileName               파일명
 * @param fileSize               파일 크기
 * @param contentType            파일 MIME 타입
 * @param thumbnailUrl           썸네일 URL
 * @param replyToMessageId       답장 대상 메시지 ID
 * @param forwardedFromMessageId 전달 원본 메시지 ID
 * @author seunggu.lee
 */
public record MessageDto(
        Long id,
        Long senderId,
        String content,
        String type,
        LocalDateTime createdAt,
        String fileUrl,
        String fileName,
        Long fileSize,
        String contentType,
        String thumbnailUrl,
        Long replyToMessageId,
        Long forwardedFromMessageId
) {

    /**
     * Message 엔티티로부터 DTO를 생성합니다.
     *
     * @param message Message 엔티티
     * @return MessageDto 인스턴스
     */
    public static MessageDto from(Message message) {
        return new MessageDto(
                message.getId(),
                message.getSenderId(),
                message.getContent(),
                message.getType().name(),
                message.getCreatedAt(),
                message.getFileUrl(),
                message.getFileName(),
                message.getFileSize(),
                message.getFileContentType(),
                message.getThumbnailUrl(),
                message.getReplyToMessageId(),
                message.getForwardedFromMessageId()
        );
    }
}
