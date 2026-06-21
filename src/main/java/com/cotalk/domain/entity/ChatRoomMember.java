package com.cotalk.domain.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 채팅방 멤버 도메인 엔티티.
 * 채팅방에 참여한 사용자의 정보를 나타낸다.
 * 순수 도메인 모델이며 JPA 어노테이션은 persistence 계층에만 존재한다.
 *
 * @author seunggu.lee
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class ChatRoomMember extends DomainBaseEntity {

    private Long id;

    private Long chatRoomId;

    private Long userId;

    @Builder.Default
    private MemberRole role = MemberRole.MEMBER;

    private LocalDateTime lastReadAt;

    private Long lastReadMessageId;

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
     * DomainBaseEntity의 createdAt을 joinedAt으로 사용한다.
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
     * 마지막 읽은 메시지 ID를 갱신한다.
     *
     * @param lastReadMessageId 마지막 읽은 메시지 ID
     */
    public void updateLastReadMessageId(Long lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
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
