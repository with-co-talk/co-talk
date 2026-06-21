package com.cotalk.domain.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


/**
 * 사용자 차단 도메인 엔티티.
 * 사용자 간의 차단 관계 정보를 나타낸다.
 * 순수 도메인 모델이며 JPA 어노테이션은 persistence 계층에만 존재한다.
 *
 * @author seunggu.lee
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class Block extends DomainBaseEntity {

    private Long id;

    private Long blockerId;

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
