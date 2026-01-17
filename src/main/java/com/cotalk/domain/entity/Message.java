package com.cotalk.domain.entity;

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
public class Message {

    @Id
    private Long id;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(nullable = false, length = 4000)
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

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

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
        FILE
    }

    /**
     * 엔티티 생성 시 호출되는 콜백 메서드.
     * 생성 시간과 수정 시간을 현재 시간으로 설정한다.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * 엔티티 수정 시 호출되는 콜백 메서드.
     * 수정 시간을 현재 시간으로 갱신한다.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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
}
