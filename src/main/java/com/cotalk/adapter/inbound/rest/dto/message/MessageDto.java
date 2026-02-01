package com.cotalk.adapter.inbound.rest.dto.message;

import com.cotalk.domain.entity.Message;
import com.cotalk.domain.util.HtmlSanitizer;

import java.time.LocalDateTime;

/**
 * 메시지 정보 DTO.
 *
 * @param id                     메시지 ID
 * @param senderId               발신자 ID
 * @param senderNickname         발신자 닉네임
 * @param senderAvatarUrl        발신자 프로필 이미지 URL
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
 * @param unreadCount            읽지 않은 멤버 수
 * @author seunggu.lee
 */
public record MessageDto(
        Long id,
        Long senderId,
        String senderNickname,
        String senderAvatarUrl,
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
        Integer unreadCount
) {

    /**
     * Message 엔티티로부터 DTO를 생성합니다. (unreadCount, senderNickname, senderAvatarUrl 포함)
     *
     * @param message         Message 엔티티
     * @param unreadCount     읽지 않은 멤버 수
     * @param senderNickname  발신자 닉네임
     * @param senderAvatarUrl 발신자 프로필 이미지 URL
     * @return MessageDto 인스턴스
     */
    public static MessageDto from(Message message, Integer unreadCount, String senderNickname, String senderAvatarUrl) {
        return new MessageDto(
                message.getId(),
                message.getSenderId(),
                senderNickname,
                senderAvatarUrl,
                // 과거 데이터 호환: 저장된 HTML 엔티티를 복원해 클라이언트에 원문으로 보여준다.
                HtmlSanitizer.unescape(message.getContent()),
                message.getType().name(),
                message.getCreatedAt(),
                message.getFileUrl(),
                message.getFileName(),
                message.getFileSize(),
                message.getFileContentType(),
                message.getThumbnailUrl(),
                message.getReplyToMessageId(),
                message.getForwardedFromMessageId(),
                unreadCount
        );
    }

    /**
     * Message 엔티티로부터 DTO를 생성합니다. (unreadCount, senderNickname 포함, senderAvatarUrl 미포함)
     *
     * @param message        Message 엔티티
     * @param unreadCount    읽지 않은 멤버 수
     * @param senderNickname 발신자 닉네임
     * @return MessageDto 인스턴스
     */
    public static MessageDto from(Message message, Integer unreadCount, String senderNickname) {
        return from(message, unreadCount, senderNickname, null);
    }

    /**
     * Message 엔티티로부터 DTO를 생성합니다. (unreadCount 포함, senderNickname, senderAvatarUrl 미포함)
     *
     * @param message     Message 엔티티
     * @param unreadCount 읽지 않은 멤버 수
     * @return MessageDto 인스턴스
     */
    public static MessageDto from(Message message, Integer unreadCount) {
        return from(message, unreadCount, null, null);
    }

    /**
     * Message 엔티티로부터 DTO를 생성합니다. (unreadCount, senderNickname, senderAvatarUrl 미포함)
     *
     * @param message Message 엔티티
     * @return MessageDto 인스턴스
     */
    public static MessageDto from(Message message) {
        return from(message, 0, null, null);
    }
}
