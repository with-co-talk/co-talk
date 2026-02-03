package com.cotalk.adapter.inbound.rest.dto.message;

import com.cotalk.domain.entity.Message;

import java.time.LocalDateTime;

/**
 * 메시지 전송 응답 DTO.
 *
 * @param messageId              메시지 ID
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
 * @param linkPreviewUrl         링크 미리보기 URL (텍스트 메시지에 URL 포함 시 비동기 수집)
 * @param linkPreviewTitle       링크 미리보기 제목
 * @param linkPreviewDescription 링크 미리보기 설명
 * @param linkPreviewImageUrl    링크 미리보기 이미지 URL
 * @author seunggu.lee
 */
public record SendMessageResponse(
        Long messageId,
        String content,
        String type,
        LocalDateTime createdAt,
        String fileUrl,
        String fileName,
        Long fileSize,
        String contentType,
        String thumbnailUrl,
        Long replyToMessageId,
        Long forwardedFromMessageId,
        String linkPreviewUrl,
        String linkPreviewTitle,
        String linkPreviewDescription,
        String linkPreviewImageUrl
) {

    /**
     * Message 엔티티로부터 응답 DTO를 생성합니다.
     *
     * @param message Message 엔티티
     * @return SendMessageResponse 인스턴스
     */
    public static SendMessageResponse from(Message message) {
        return new SendMessageResponse(
                message.getId(),
                message.getContent(),
                message.getType().name(),
                message.getCreatedAt(),
                message.getFileUrl(),
                message.getFileName(),
                message.getFileSize(),
                message.getFileContentType(),
                message.getThumbnailUrl(),
                message.getReplyToMessageId(),
                message.getForwardedFromMessageId(),
                message.getLinkPreviewUrl(),
                message.getLinkPreviewTitle(),
                message.getLinkPreviewDescription(),
                message.getLinkPreviewImageUrl()
        );
    }
}
