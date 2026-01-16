package com.cotalk.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    public enum MessageType {
        TEXT, IMAGE, FILE
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void validateContent() {
        if (type == MessageType.TEXT && (content == null || content.trim().isEmpty())) {
            throw new IllegalArgumentException("메시지 내용은 비어있을 수 없습니다.");
        }
        if ((type == MessageType.IMAGE || type == MessageType.FILE) && (fileUrl == null || fileUrl.trim().isEmpty())) {
            throw new IllegalArgumentException("파일 URL은 비어있을 수 없습니다.");
        }
    }

    /**
     * 이미지 메시지인지 확인
     */
    public boolean isImage() {
        return type == MessageType.IMAGE;
    }

    /**
     * 파일 메시지인지 확인
     */
    public boolean isFile() {
        return type == MessageType.FILE;
    }

    /**
     * 텍스트 메시지인지 확인
     */
    public boolean isText() {
        return type == MessageType.TEXT;
    }

    /**
     * 메시지 내용 수정
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
     * 메시지 삭제 (소프트 삭제)
     */
    public void delete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 본인이 보낸 메시지인지 확인
     */
    public boolean isSentBy(Long userId) {
        return this.senderId.equals(userId);
    }

    /**
     * 삭제된 메시지인지 확인
     */
    public boolean isDeleted() {
        return deleted;
    }
}
