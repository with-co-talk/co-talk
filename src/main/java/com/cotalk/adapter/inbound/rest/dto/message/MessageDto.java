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
 * @param fileContentType        파일 MIME 타입
 * @param thumbnailUrl           썸네일 URL
 * @param replyToMessageId       답장 대상 메시지 ID
 * @param forwardedFromMessageId 전달 원본 메시지 ID
 * @param unreadCount            읽지 않은 멤버 수
 * @param linkPreviewUrl         링크 미리보기 URL
 * @param linkPreviewTitle       링크 미리보기 제목
 * @param linkPreviewDescription 링크 미리보기 설명
 * @param linkPreviewImageUrl    링크 미리보기 이미지 URL
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
        String fileContentType,
        String thumbnailUrl,
        Long replyToMessageId,
        Long forwardedFromMessageId,
        Integer unreadCount,
        String linkPreviewUrl,
        String linkPreviewTitle,
        String linkPreviewDescription,
        String linkPreviewImageUrl
) {

    /**
     * Message 엔티티로부터 DTO를 생성합니다. (unreadCount, senderNickname, senderAvatarUrl 포함)
     * <p>
     * 첨부파일 URL은 엔티티에 저장된 원본 값을 사용합니다. 멤버십 검증 후 재발급된 단기 Pre-signed URL을
     * 노출하려면 {@link #from(Message, Integer, String, String, String, String)}을 사용하세요(H-1).
     * </p>
     *
     * @param message         Message 엔티티
     * @param unreadCount     읽지 않은 멤버 수
     * @param senderNickname  발신자 닉네임
     * @param senderAvatarUrl 발신자 프로필 이미지 URL
     * @return MessageDto 인스턴스
     */
    public static MessageDto from(Message message, Integer unreadCount, String senderNickname, String senderAvatarUrl) {
        return from(message, unreadCount, senderNickname, senderAvatarUrl,
                message.getFileUrl(), message.getThumbnailUrl());
    }

    /**
     * Message 엔티티로부터 DTO를 생성합니다. (첨부파일 URL을 명시적으로 지정)
     * <p>
     * 멤버십이 검증된 읽기 경로에서 재발급한 단기 Pre-signed URL({@code fileUrl}/{@code thumbnailUrl})을
     * 엔티티에 저장된 원본 URL 대신 클라이언트에 노출하기 위해 사용합니다(H-1).
     * </p>
     *
     * @param message         Message 엔티티
     * @param unreadCount     읽지 않은 멤버 수
     * @param senderNickname  발신자 닉네임
     * @param senderAvatarUrl 발신자 프로필 이미지 URL
     * @param fileUrl         클라이언트에 노출할 파일 URL (단기 Pre-signed URL)
     * @param thumbnailUrl    클라이언트에 노출할 썸네일 URL (단기 Pre-signed URL)
     * @return MessageDto 인스턴스
     */
    public static MessageDto from(Message message, Integer unreadCount, String senderNickname, String senderAvatarUrl,
                                  String fileUrl, String thumbnailUrl) {
        return new MessageDto(
                message.getId(),
                message.getSenderId(),
                senderNickname,
                senderAvatarUrl,
                // 과거 데이터 호환: 저장된 HTML 엔티티를 복원해 클라이언트에 원문으로 보여준다.
                HtmlSanitizer.unescape(message.getContent()),
                message.getType().name(),
                message.getCreatedAt(),
                fileUrl,
                message.getFileName(),
                message.getFileSize(),
                message.getFileContentType(),
                thumbnailUrl,
                message.getReplyToMessageId(),
                message.getForwardedFromMessageId(),
                unreadCount,
                message.getLinkPreviewUrl(),
                message.getLinkPreviewTitle(),
                message.getLinkPreviewDescription(),
                message.getLinkPreviewImageUrl()
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
