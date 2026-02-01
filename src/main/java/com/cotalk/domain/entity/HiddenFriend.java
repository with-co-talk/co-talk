package com.cotalk.domain.entity;

import jakarta.persistence.*;
import lombok.*;


/**
 * 친구 숨김 엔티티.
 * 사용자가 특정 친구를 숨긴 관계 정보를 나타낸다.
 *
 * @author seunggu.lee
 */
@Entity
@Table(name = "hidden_friends", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "friend_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HiddenFriend extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "friend_id", nullable = false)
    private Long friendId;

    /**
     * 지정된 사용자가 이 숨김 관계의 소유자인지 확인한다.
     *
     * @param userId 확인할 사용자 ID
     * @return 소유자이면 true, 그렇지 않으면 false
     */
    public boolean isHiddenBy(Long userId) {
        return this.userId.equals(userId);
    }
}
