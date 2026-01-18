package com.cotalk.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 친구 요청 엔티티.
 * 사용자 간의 친구 요청 정보를 나타낸다.
 *
 * @author seunggu.lee
 */
@Entity
@Table(name = "friend_requests", uniqueConstraints = {
        @UniqueConstraint(name = "uk_friend_request_requester_receiver",
                columnNames = {"requester_id", "receiver_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FriendRequest extends BaseEntity {

    @Id
    private Long id;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    /**
     * 친구 요청 상태를 나타내는 열거형.
     *
     * @author seunggu.lee
     */
    public enum RequestStatus {
        /** 대기 중 상태 */
        PENDING,
        /** 수락됨 상태 */
        ACCEPTED,
        /** 거절됨 상태 */
        REJECTED
    }

    /**
     * 요청이 대기 상태인지 확인한다.
     *
     * @return 대기 상태이면 true, 그렇지 않으면 false
     */
    public boolean isPending() {
        return status == RequestStatus.PENDING;
    }

    /**
     * 친구 요청을 수락한다.
     *
     * @throws IllegalStateException 대기 중인 요청이 아닌 경우
     */
    public void accept() {
        if (!isPending()) {
            throw new IllegalStateException("대기 중인 요청만 수락할 수 있습니다.");
        }
        this.status = RequestStatus.ACCEPTED;
    }

    /**
     * 친구 요청을 거절한다.
     *
     * @throws IllegalStateException 대기 중인 요청이 아닌 경우
     */
    public void reject() {
        if (!isPending()) {
            throw new IllegalStateException("대기 중인 요청만 거절할 수 있습니다.");
        }
        this.status = RequestStatus.REJECTED;
    }
}
