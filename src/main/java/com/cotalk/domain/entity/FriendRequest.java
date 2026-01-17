package com.cotalk.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 친구 요청 엔티티.
 * 사용자 간의 친구 요청 정보를 나타낸다.
 *
 * @author seunggu.lee
 */
@Entity
@Table(name = "friend_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FriendRequest {

    @Id
    private Long id;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

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
