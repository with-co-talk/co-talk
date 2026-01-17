package com.cotalk.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 친구 관계 엔티티.
 * 두 사용자 간의 친구 관계 정보를 나타낸다.
 *
 * @author seunggu.lee
 */
@Entity
@Table(name = "friends", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "friend_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Friend extends BaseEntity {

    @Id
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "friend_id", nullable = false)
    private Long friendId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendStatus status;

    /**
     * 친구 관계 상태를 나타내는 열거형.
     *
     * @author seunggu.lee
     */
    public enum FriendStatus {
        /** 대기 중 상태 */
        PENDING,
        /** 수락됨 상태 */
        ACCEPTED,
        /** 차단됨 상태 */
        BLOCKED
    }

    /**
     * 친구 관계가 수락 상태인지 확인한다.
     *
     * @return 수락 상태이면 true, 그렇지 않으면 false
     */
    public boolean isAccepted() {
        return status == FriendStatus.ACCEPTED;
    }

    /**
     * 친구 관계가 차단 상태인지 확인한다.
     *
     * @return 차단 상태이면 true, 그렇지 않으면 false
     */
    public boolean isBlocked() {
        return status == FriendStatus.BLOCKED;
    }

    /**
     * 친구 관계가 대기 상태인지 확인한다.
     *
     * @return 대기 상태이면 true, 그렇지 않으면 false
     */
    public boolean isPending() {
        return status == FriendStatus.PENDING;
    }

    /**
     * 지정된 사용자가 이 친구 관계의 당사자인지 확인한다.
     *
     * @param checkUserId 확인할 사용자 ID
     * @return 당사자이면 true, 그렇지 않으면 false
     */
    public boolean involves(Long checkUserId) {
        return userId.equals(checkUserId) || friendId.equals(checkUserId);
    }

    /**
     * 지정된 사용자의 친구 ID를 반환한다.
     *
     * @param currentUserId 현재 사용자 ID
     * @return 상대방 사용자 ID
     * @throws IllegalArgumentException 사용자가 이 친구 관계에 속하지 않는 경우
     */
    public Long getOtherUserId(Long currentUserId) {
        if (userId.equals(currentUserId)) {
            return friendId;
        } else if (friendId.equals(currentUserId)) {
            return userId;
        }
        throw new IllegalArgumentException("사용자가 이 친구 관계에 속하지 않습니다.");
    }
}
