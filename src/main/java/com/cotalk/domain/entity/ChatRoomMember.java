package com.cotalk.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 채팅방 멤버 엔티티.
 * 채팅방에 참여한 사용자의 정보를 나타낸다.
 *
 * @author seunggu.lee
 */
@Entity
@Table(name = "chat_room_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"chat_room_id", "user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoomMember {

    @Id
    private Long id;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MemberRole role = MemberRole.MEMBER;

    private LocalDateTime lastReadAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    /**
     * 채팅방 멤버 역할을 나타내는 열거형.
     *
     * @author seunggu.lee
     */
    public enum MemberRole {
        /** 채팅방 관리자 (방장) */
        ADMIN,
        /** 일반 멤버 */
        MEMBER
    }

    /**
     * 엔티티 생성 시 호출되는 콜백 메서드.
     * 참여 시간을 현재 시간으로 설정하고, 역할이 없으면 일반 멤버로 설정한다.
     */
    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
        if (role == null) {
            role = MemberRole.MEMBER;
        }
    }

    /**
     * 마지막 읽은 시간을 갱신한다.
     *
     * @param lastReadAt 마지막 읽은 시간
     */
    public void updateLastReadAt(LocalDateTime lastReadAt) {
        this.lastReadAt = lastReadAt;
    }

    /**
     * 관리자 여부를 확인한다.
     *
     * @return 관리자이면 true, 그렇지 않으면 false
     */
    public boolean isAdmin() {
        return role == MemberRole.ADMIN;
    }

    /**
     * 관리자로 승격한다.
     */
    public void promoteToAdmin() {
        this.role = MemberRole.ADMIN;
    }

    /**
     * 일반 멤버로 강등한다.
     */
    public void demoteToMember() {
        this.role = MemberRole.MEMBER;
    }
}
