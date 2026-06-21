package com.cotalk.adapter.outbound.persistence.entity;

import com.cotalk.domain.entity.Message;
import com.cotalk.infrastructure.crypto.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 메시지 JPA 엔티티.
 * persistence 계층 전용이며, 도메인 Message와 매핑된다.
 *
 * <p>{@code content}는 {@link EncryptedStringConverter}로 DB 저장 시 암호화되고
 * 조회 시 복호화된다 (저장 시 암호화 at-rest).</p>
 *
 * @author seunggu.lee
 */
@Entity
@Table(name = "messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MessageJpaEntity extends BaseJpaEntity {

    @Id
    private Long id;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(nullable = false, length = 4000)
    @Convert(converter = EncryptedStringConverter.class)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Message.MessageType type = Message.MessageType.TEXT;

    // 파일/이미지 메시지용 필드
    @Column(name = "file_url", length = Message.URL_MAX_LENGTH)
    private String fileUrl;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_content_type")
    private String fileContentType;

    // 이미지 미리보기용 (썸네일)
    @Column(name = "thumbnail_url", length = Message.URL_MAX_LENGTH)
    private String thumbnailUrl;

    // 답장 기능: 답장 대상 메시지 ID
    @Column(name = "reply_to_message_id")
    private Long replyToMessageId;

    // 전달 기능: 원본 메시지 ID
    @Column(name = "forwarded_from_message_id")
    private Long forwardedFromMessageId;

    // 링크 미리보기 (텍스트 메시지에 URL 포함 시 비동기 수집)
    @Column(name = "link_preview_url", length = 2048)
    private String linkPreviewUrl;

    @Column(name = "link_preview_title", length = 512)
    private String linkPreviewTitle;

    @Column(name = "link_preview_description", length = 1000)
    private String linkPreviewDescription;

    @Column(name = "link_preview_image_url", length = 2048)
    private String linkPreviewImageUrl;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;
}
