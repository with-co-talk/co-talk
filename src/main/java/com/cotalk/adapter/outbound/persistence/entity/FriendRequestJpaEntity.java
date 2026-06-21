package com.cotalk.adapter.outbound.persistence.entity;

import com.cotalk.domain.entity.FriendRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 친구 요청 JPA 엔티티.
 * persistence 계층 전용이며, 도메인 FriendRequest와 매핑된다.
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
public class FriendRequestJpaEntity extends BaseJpaEntity {

    @Id
    private Long id;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendRequest.RequestStatus status;
}
