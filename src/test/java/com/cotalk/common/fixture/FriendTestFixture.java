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

    /**
     * 빌더 스타일로 Friend 생성을 시작합니다.
     *
     * @return FriendBuilder 인스턴스
     */
    public static FriendBuilder builder() {
        return new FriendBuilder();
    }

    /**
     * Friend 테스트 빌더.
     */
    public static class FriendBuilder {
        private Long id = 1L;
        private Long userId = 1L;
        private Long friendId = 2L;
        private Friend.FriendStatus status = Friend.FriendStatus.ACCEPTED;

        /**
         * 친구 관계 ID를 설정한다.
         *
         * @param id 친구 관계 ID
         * @return 빌더
         */
        public FriendBuilder id(Long id) {
            this.id = id;
            return this;
        }

        /**
         * 사용자 ID를 설정한다.
         *
         * @param userId 사용자 ID
         * @return 빌더
         */
        public FriendBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * 친구 ID를 설정한다.
         *
         * @param friendId 친구 ID
         * @return 빌더
         */
        public FriendBuilder friendId(Long friendId) {
            this.friendId = friendId;
            return this;
        }

        /**
         * 친구 관계 상태를 설정한다.
         *
         * @param status 친구 관계 상태
         * @return 빌더
         */
        public FriendBuilder status(Friend.FriendStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Friend 객체를 생성한다.
         *
         * @return 생성된 Friend 엔티티
         */
        public Friend build() {
            return Friend.builder()
                    .id(id)
                    .userId(userId)
                    .friendId(friendId)
                    .status(status)
                    .build();
        }
    }
}
