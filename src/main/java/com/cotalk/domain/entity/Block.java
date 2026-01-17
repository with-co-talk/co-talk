package com.cotalk.domain.entity;

import jakarta.persistence.*;
import lombok.*;


/**
 * 사용자 차단 엔티티.
 * 사용자 간의 차단 관계 정보를 나타낸다.
 *
 * @author seunggu.lee
 */
@Entity
@Table(name = "blocks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"blocker_id", "blocked_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Block extends BaseEntity {

    @Id
    private Long id;

    @Column(name = "blocker_id", nullable = false)
    private Long blockerId;

    @Column(name = "blocked_id", nullable = false)
    private Long blockedId;

    /**
     * 지정된 사용자가 이 차단 관계의 차단자인지 확인한다.
     *
     * @param userId 확인할 사용자 ID
     * @return 차단자이면 true, 그렇지 않으면 false
     */
    public boolean isBlockedBy(Long userId) {
        return blockerId.equals(userId);
    }
}
