package com.cotalk.common.fixture;

import com.cotalk.domain.entity.Friend;

/**
 * Friend 엔티티 테스트 픽스처
 * 테스트에서 반복적으로 사용되는 Friend 객체 생성 메서드를 제공합니다.
 */
public class FriendTestFixture {

    /**
     * 기본값으로 Friend 객체를 생성합니다.
     * (id=1, userId=1, friendId=2, status=ACCEPTED)
     */
    public static Friend createFriend() {
        return createFriend(1L, 1L, 2L);
    }

    /**
     * 지정된 ID와 사용자 정보로 Friend 객체를 생성합니다.
     * 상태는 ACCEPTED로 설정됩니다.
     *
     * @param id       친구 관계 ID
     * @param userId   사용자 ID
     * @param friendId 친구 ID
     * @return ACCEPTED 상태의 Friend 엔티티
     */
    public static Friend createFriend(Long id, Long userId, Long friendId) {
        return Friend.builder()
                .id(id)
                .userId(userId)
                .friendId(friendId)
                .status(Friend.FriendStatus.ACCEPTED)
                .build();
    }

    /**
     * PENDING 상태의 Friend 객체를 생성합니다.
     *
     * @param id       친구 관계 ID
     * @param userId   사용자 ID
     * @param friendId 친구 ID
     * @return PENDING 상태의 Friend 엔티티
     */
    public static Friend createPendingFriend(Long id, Long userId, Long friendId) {
        return Friend.builder()
                .id(id)
                .userId(userId)
                .friendId(friendId)
                .status(Friend.FriendStatus.PENDING)
                .build();
    }

    /**
     * BLOCKED 상태의 Friend 객체를 생성합니다.
     *
     * @param id       친구 관계 ID
     * @param userId   사용자 ID
     * @param friendId 친구 ID
     * @return BLOCKED 상태의 Friend 엔티티
     */
    public static Friend createBlockedFriend(Long id, Long userId, Long friendId) {
        return Friend.builder()
                .id(id)
                .userId(userId)
                .friendId(friendId)
                .status(Friend.FriendStatus.BLOCKED)
                .build();
    }
}
