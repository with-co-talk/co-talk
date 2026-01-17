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
@AttributeOverride(name = "createdAt", column = @Column(name = "joined_at", nullable = false, updatable = false))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoomMember extends BaseEntity {

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
     * 참여 시간을 반환한다.
     * BaseEntity의 createdAt을 joinedAt으로 사용한다.
     *
     * @return 참여 시간
     */
    public LocalDateTime getJoinedAt() {
        return getCreatedAt();
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
