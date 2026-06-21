package com.cotalk.domain.entity;

import com.cotalk.domain.constants.MessageConstants;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 메시지 도메인 엔티티.
 * 채팅방에서 주고받는 메시지 정보를 나타낸다.
 * 텍스트, 이미지, 파일 메시지를 지원한다.
 * 순수 도메인 모델이며 JPA 어노테이션은 persistence 계층에만 존재한다.
 *
 * @author seunggu.lee
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class Message extends DomainBaseEntity {

    public static final int URL_MAX_LENGTH = 2048;

    private Long id;

    private Long chatRoomId;

    private Long senderId;

    private String content;

    @Builder.Default
    private MessageType type = MessageType.TEXT;

    // 파일/이미지 메시지용 필드
    private String fileUrl;

    private String fileName;

    private Long fileSize;

    private String fileContentType;

    // 이미지 미리보기용 (썸네일)
    private String thumbnailUrl;

    // 답장 기능: 답장 대상 메시지 ID
    private Long replyToMessageId;

    // 전달 기능: 원본 메시지 ID
    private Long forwardedFromMessageId;

    // 링크 미리보기 (텍스트 메시지에 URL 포함 시 비동기 수집)
    private String linkPreviewUrl;

    private String linkPreviewTitle;

    private String linkPreviewDescription;

    private String linkPreviewImageUrl;

    private LocalDateTime deletedAt;

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
     *
     * @param now 현재 시간
     */
    public void delete(LocalDateTime now) {
        this.deleted = true;
        this.deletedAt = now;
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
     * 링크 미리보기 정보를 적용한다.
     * 텍스트 메시지에 URL이 포함된 경우 비동기로 수집된 메타데이터를 저장할 때 사용한다.
     *
     * @param url         미리보기 대상 URL
     * @param title       페이지 제목
     * @param description 페이지 설명
     * @param imageUrl    대표 이미지 URL
     */
    public void applyLinkPreview(String url, String title, String description, String imageUrl) {
        this.linkPreviewUrl = url;
        this.linkPreviewTitle = title;
        this.linkPreviewDescription = description;
        this.linkPreviewImageUrl = imageUrl;
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
     * @param now 현재 시간
     * @return 5분이 지났으면 true, 아직 수정/삭제 가능하면 false
     */
    public boolean isEditTimeExpired(LocalDateTime now) {
        if (getCreatedAt() == null) {
            return false;
        }
        return getCreatedAt().plusMinutes(MessageConstants.EDIT_TIME_LIMIT_MINUTES).isBefore(now);
    }
}
