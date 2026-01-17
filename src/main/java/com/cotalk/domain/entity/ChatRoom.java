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
public class ChatRoom {

    @Id
    private Long id;

    private String name;

    @Column(length = 500)
    private String announcement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomType type;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 채팅방 유형을 나타내는 열거형.
     *
     * @author seunggu.lee
     */
    public enum ChatRoomType {
        /** 1:1 채팅방 */
        DIRECT,
        /** 그룹 채팅방 */
        GROUP
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
     * 채팅방 이름을 변경한다.
     *
     * @param newName 새 채팅방 이름
     */
    public void updateName(String newName) {
        this.name = newName;
    }

    /**
     * 공지사항을 설정한다.
     *
     * @param announcement 설정할 공지사항
     */
    public void setAnnouncement(String announcement) {
        this.announcement = announcement;
    }

    /**
     * 공지사항을 삭제한다.
     */
    public void clearAnnouncement() {
        this.announcement = null;
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
