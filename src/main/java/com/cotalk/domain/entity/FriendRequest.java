package com.cotalk.domain.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 친구 요청 도메인 엔티티.
 * 사용자 간의 친구 요청 정보를 나타낸다.
 * 순수 도메인 모델이며 JPA 어노테이션은 persistence 계층에만 존재한다.
 *
 * @author seunggu.lee
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class FriendRequest extends DomainBaseEntity {

    private Long id;

    private Long requesterId;

    private Long receiverId;

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
