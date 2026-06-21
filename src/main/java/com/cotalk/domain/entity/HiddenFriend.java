package com.cotalk.domain.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


/**
 * 친구 숨김 도메인 엔티티.
 * 사용자가 특정 친구를 숨긴 관계 정보를 나타낸다.
 * 순수 도메인 모델이며 JPA 어노테이션은 persistence 계층에만 존재한다.
 *
 * @author seunggu.lee
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class HiddenFriend extends DomainBaseEntity {

    private Long id;

    private Long userId;

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
