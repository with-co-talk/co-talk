package com.cotalk.domain.entity;

import com.cotalk.infrastructure.crypto.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 메시지 엔티티.
 * 채팅방에서 주고받는 메시지 정보를 나타낸다.
 * 텍스트, 이미지, 파일 메시지를 지원한다.
 *
 * @author seunggu.lee
 */
@Entity
@Table(name = "messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Message extends BaseEntity {

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
    private MessageType type = MessageType.TEXT;

    // 파일/이미지 메시지용 필드
    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_content_type")
    private String fileContentType;

    // 이미지 미리보기용 (썸네일)
    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    // 답장 기능: 답장 대상 메시지 ID
    @Column(name = "reply_to_message_id")
    private Long replyToMessageId;

    // 전달 기능: 원본 메시지 ID
    @Column(name = "forwarded_from_message_id")
    private Long forwardedFromMessageId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    /**
     * 메시지 유형을 나타내는 열거형.
     *
     * @author seunggu.lee
     */
    public enum MessageType {
        /** 텍스트 메시지 */
        TEXT,
        /** 이미지 메시지 */
        IMAGE,
        /** 파일 메시지 */
        FILE,
        /** 시스템 메시지 (입장/퇴장 알림 등) */
        SYSTEM
    }

    /**
     * 메시지 내용의 유효성을 검증한다.
     * 텍스트 메시지는 내용이 필수이며, 이미지/파일 메시지는 파일 URL이 필수이다.
     *
     * @throws IllegalArgumentException 유효성 검증에 실패한 경우
     */
    public void validateContent() {
        if (type == MessageType.TEXT && (content == null || content.trim().isEmpty())) {
            throw new IllegalArgumentException("메시지 내용은 비어있을 수 없습니다.");
        }
        if ((type == MessageType.IMAGE || type == MessageType.FILE) && (fileUrl == null || fileUrl.trim().isEmpty())) {
            throw new IllegalArgumentException("파일 URL은 비어있을 수 없습니다.");
        }
    }

    /**
     * 이미지 메시지인지 확인한다.
     *
     * @return 이미지 메시지이면 true, 그렇지 않으면 false
     */
    public boolean isImage() {
        return type == MessageType.IMAGE;
    }

    /**
     * 파일 메시지인지 확인한다.
     *
     * @return 파일 메시지이면 true, 그렇지 않으면 false
     */
    public boolean isFile() {
        return type == MessageType.FILE;
    }

    /**
     * 텍스트 메시지인지 확인한다.
     *
     * @return 텍스트 메시지이면 true, 그렇지 않으면 false
     */
    public boolean isText() {
        return type == MessageType.TEXT;
    }

    /**
     * 메시지 내용을 수정한다.
     * 삭제된 메시지나 텍스트가 아닌 메시지는 수정할 수 없다.
     *
     * @param newContent 새 메시지 내용
     * @throws IllegalStateException 삭제된 메시지이거나 텍스트 메시지가 아닌 경우
     * @throws IllegalArgumentException 새 내용이 null이거나 비어있는 경우
     */
    public void updateContent(String newContent) {
        if (deleted) {
            throw new IllegalStateException("삭제된 메시지는 수정할 수 없습니다.");
        }
        if (type != MessageType.TEXT) {
            throw new IllegalStateException("텍스트 메시지만 수정할 수 있습니다.");
        }
        if (newContent == null || newContent.trim().isEmpty()) {
            throw new IllegalArgumentException("메시지 내용은 비어있을 수 없습니다.");
        }
        this.content = newContent;
    }

    /**
     * 메시지를 삭제한다 (소프트 삭제).
     * 실제로 데이터베이스에서 삭제되지 않고 삭제 플래그와 삭제 시간이 설정된다.
     */
    public void delete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 지정된 사용자가 보낸 메시지인지 확인한다.
     *
     * @param userId 확인할 사용자 ID
     * @return 해당 사용자가 보낸 메시지이면 true, 그렇지 않으면 false
     */
    public boolean isSentBy(Long userId) {
        return this.senderId.equals(userId);
    }

    /**
     * 삭제된 메시지인지 확인한다.
     *
     * @return 삭제된 메시지이면 true, 그렇지 않으면 false
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * 시스템 메시지인지 확인한다.
     *
     * @return 시스템 메시지이면 true, 그렇지 않으면 false
     */
    public boolean isSystem() {
        return type == MessageType.SYSTEM;
    }

    /**
     * 시스템 메시지를 생성한다.
     *
     * @param id 메시지 ID
     * @param chatRoomId 채팅방 ID
     * @param content 시스템 메시지 내용
     * @return 생성된 시스템 메시지
     */
    public static Message createSystemMessage(Long id, Long chatRoomId, String content) {
        return Message.builder()
                .id(id)
                .chatRoomId(chatRoomId)
                .senderId(0L) // 시스템 메시지는 senderId를 0으로 설정
                .content(content)
                .type(MessageType.SYSTEM)
                .build();
    }

    /**
     * 메시지 수정/삭제 가능 시간(5분)이 지났는지 확인한다.
     *
     * @return 5분이 지났으면 true, 아직 수정/삭제 가능하면 false
     */
    public boolean isEditTimeExpired() {
        if (getCreatedAt() == null) {
            return false;
        }
        return getCreatedAt().plusMinutes(5).isBefore(LocalDateTime.now());
    }
}
