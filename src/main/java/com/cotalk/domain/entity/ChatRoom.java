package com.cotalk.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 채팅방 엔티티.
 * 1:1 채팅 및 그룹 채팅방 정보를 나타낸다.
 *
 * @author seunggu.lee
 */
@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoom extends BaseEntity {

    @Id
    private Long id;

    private String name;

    @Column(length = 500)
    private String announcement;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomType type;

    /**
     * 채팅방 유형을 나타내는 열거형.
     *
     * @author seunggu.lee
     */
    public enum ChatRoomType {
        /** 1:1 채팅방 */
        DIRECT,
        /** 그룹 채팅방 */
        GROUP,
        /** 나와의 채팅방 (자기 자신과의 메모용 채팅) */
        SELF
    }

    /**
     * 채팅방 이름을 변경한다.
     *
     * @param newName 새 채팅방 이름
     */
    public void updateName(String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("채팅방 이름은 필수입니다.");
        }
        if (newName.length() > 50) {
            throw new IllegalArgumentException("채팅방 이름은 50자를 초과할 수 없습니다.");
        }
        this.name = newName;
    }

    /**
     * 공지사항을 설정한다.
     *
     * @param announcement 설정할 공지사항
     */
    public void setAnnouncement(String announcement) {
        if (announcement != null && announcement.length() > 500) {
            throw new IllegalArgumentException("공지사항은 500자를 초과할 수 없습니다.");
        }
        this.announcement = announcement;
    }

    /**
     * 공지사항을 삭제한다.
     */
    public void clearAnnouncement() {
        this.announcement = null;
    }

    /**
     * 채팅방 이미지를 변경한다.
     *
     * @param imageUrl 새 이미지 URL
     */
    public void updateImageUrl(String imageUrl) {
        if (imageUrl != null && imageUrl.length() > 500) {
            throw new IllegalArgumentException("이미지 URL은 500자를 초과할 수 없습니다.");
        }
        this.imageUrl = imageUrl;
    }

    /**
     * 1:1 채팅방인지 확인한다.
     *
     * @return 1:1 채팅방이면 true, 그렇지 않으면 false
     */
    public boolean isDirectChat() {
        return type == ChatRoomType.DIRECT;
    }

    /**
     * 그룹 채팅방인지 확인한다.
     *
     * @return 그룹 채팅방이면 true, 그렇지 않으면 false
     */
    public boolean isGroupChat() {
        return type == ChatRoomType.GROUP;
    }

    /**
     * 나와의 채팅방인지 확인한다.
     *
     * @return 나와의 채팅방이면 true, 그렇지 않으면 false
     */
    public boolean isSelfChat() {
        return type == ChatRoomType.SELF;
    }

    /**
     * 그룹 채팅방의 경우 이름이 필수인지 확인한다.
     *
     * @return 이름이 필수이면 true, 그렇지 않으면 false
     */
    public boolean requiresName() {
        return isGroupChat();
    }

    /**
     * 채팅방 이름을 반환한다.
     * 1:1 채팅방의 경우 null을 반환할 수 있다.
     *
     * @return 채팅방 이름
     */
    public String getDisplayName() {
        return name;
    }

    /**
     * 채팅방 이름의 유효성을 검증한다.
     * 1:1 채팅방은 이름이 없어도 유효하며,
     * 그룹 채팅방은 이름이 필수이다.
     *
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public boolean isValidName() {
        if (isDirectChat()) {
            return true; // 1:1 채팅방은 이름이 없어도 됨
        }
        return name != null && !name.trim().isEmpty();
    }
}
