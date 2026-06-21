package com.cotalk.adapter.outbound.persistence.entity;

import com.cotalk.domain.entity.Friend;
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
 * 친구 관계 JPA 엔티티.
 * persistence 계층 전용이며, 도메인 Friend와 매핑된다.
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
public class FriendJpaEntity extends BaseJpaEntity {

    @Id
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "friend_id", nullable = false)
    private Long friendId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Friend.FriendStatus status;
}
