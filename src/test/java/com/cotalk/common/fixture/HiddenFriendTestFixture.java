package com.cotalk.common.fixture;

import com.cotalk.domain.entity.HiddenFriend;

/**
 * HiddenFriend 엔티티 테스트 픽스처.
 * 테스트에서 반복적으로 사용되는 HiddenFriend 객체 생성 메서드를 제공한다.
 *
 * @author seunggu.lee
 */
public class HiddenFriendTestFixture {

    private static final Long DEFAULT_ID = 1L;
    private static final Long DEFAULT_USER_ID = 1L;
    private static final Long DEFAULT_FRIEND_ID = 2L;

    /**
     * 기본값으로 HiddenFriend 객체를 생성한다.
     * (id=1, userId=1, friendId=2)
     *
     * @return HiddenFriend 엔티티
     */
    public static HiddenFriend createHiddenFriend() {
        return createHiddenFriend(DEFAULT_ID, DEFAULT_USER_ID, DEFAULT_FRIEND_ID);
    }

    /**
     * 지정된 사용자 ID와 친구 ID로 HiddenFriend 객체를 생성한다.
     *
     * @param userId   사용자 ID
     * @param friendId 숨길 친구 ID
     * @return HiddenFriend 엔티티
     */
    public static HiddenFriend createHiddenFriend(Long userId, Long friendId) {
        return createHiddenFriend(DEFAULT_ID, userId, friendId);
    }

    /**
     * 지정된 ID, 사용자 ID, 친구 ID로 HiddenFriend 객체를 생성한다.
     *
     * @param id       숨김 ID
     * @param userId   사용자 ID
     * @param friendId 숨길 친구 ID
     * @return HiddenFriend 엔티티
     */
    public static HiddenFriend createHiddenFriend(Long id, Long userId, Long friendId) {
        return HiddenFriend.builder()
                .id(id)
                .userId(userId)
                .friendId(friendId)
                .build();
    }

    /**
     * 빌더 스타일로 HiddenFriend 생성을 시작한다.
     *
     * @return HiddenFriendBuilder 인스턴스
     */
    public static HiddenFriendBuilder builder() {
        return new HiddenFriendBuilder();
    }

    /**
     * HiddenFriend 테스트 빌더.
     */
    public static class HiddenFriendBuilder {
        private Long id = DEFAULT_ID;
        private Long userId = DEFAULT_USER_ID;
        private Long friendId = DEFAULT_FRIEND_ID;

        /**
         * 숨김 ID를 설정한다.
         *
         * @param id 숨김 ID
         * @return 빌더
         */
        public HiddenFriendBuilder id(Long id) {
            this.id = id;
            return this;
        }

        /**
         * 사용자 ID를 설정한다.
         *
         * @param userId 사용자 ID
         * @return 빌더
         */
        public HiddenFriendBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        /**
         * 숨길 친구 ID를 설정한다.
         *
         * @param friendId 숨길 친구 ID
         * @return 빌더
         */
        public HiddenFriendBuilder friendId(Long friendId) {
            this.friendId = friendId;
            return this;
        }

        /**
         * HiddenFriend 객체를 생성한다.
         *
         * @return 생성된 HiddenFriend 엔티티
         */
        public HiddenFriend build() {
            return HiddenFriend.builder()
                    .id(id)
                    .userId(userId)
                    .friendId(friendId)
                    .build();
        }
    }
}
