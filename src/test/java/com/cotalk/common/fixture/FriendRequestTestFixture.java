package com.cotalk.common.fixture;

import com.cotalk.domain.entity.FriendRequest;

/**
 * FriendRequest 엔티티 테스트 픽스처
 * 테스트에서 반복적으로 사용되는 FriendRequest 객체 생성 메서드를 제공합니다.
 */
public class FriendRequestTestFixture {

    /**
     * 기본값으로 PENDING 상태의 FriendRequest 객체를 생성합니다.
     * (id=1, requesterId=1, receiverId=2)
     */
    public static FriendRequest createPendingRequest() {
        return createPendingRequest(1L, 1L, 2L);
    }

    /**
     * 지정된 ID와 사용자 정보로 PENDING 상태의 FriendRequest 객체를 생성합니다.
     *
     * @param id          요청 ID
     * @param requesterId 요청자 ID
     * @param receiverId  수신자 ID
     * @return PENDING 상태의 FriendRequest 엔티티
     */
    public static FriendRequest createPendingRequest(Long id, Long requesterId, Long receiverId) {
        return FriendRequest.builder()
                .id(id)
                .requesterId(requesterId)
                .receiverId(receiverId)
                .status(FriendRequest.RequestStatus.PENDING)
                .build();
    }

    /**
     * 지정된 ID와 사용자 정보로 ACCEPTED 상태의 FriendRequest 객체를 생성합니다.
     *
     * @param id          요청 ID
     * @param requesterId 요청자 ID
     * @param receiverId  수신자 ID
     * @return ACCEPTED 상태의 FriendRequest 엔티티
     */
    public static FriendRequest createAcceptedRequest(Long id, Long requesterId, Long receiverId) {
        return FriendRequest.builder()
                .id(id)
                .requesterId(requesterId)
                .receiverId(receiverId)
                .status(FriendRequest.RequestStatus.ACCEPTED)
                .build();
    }

    /**
     * 지정된 ID와 사용자 정보로 REJECTED 상태의 FriendRequest 객체를 생성합니다.
     *
     * @param id          요청 ID
     * @param requesterId 요청자 ID
     * @param receiverId  수신자 ID
     * @return REJECTED 상태의 FriendRequest 엔티티
     */
    public static FriendRequest createRejectedRequest(Long id, Long requesterId, Long receiverId) {
        return FriendRequest.builder()
                .id(id)
                .requesterId(requesterId)
                .receiverId(receiverId)
                .status(FriendRequest.RequestStatus.REJECTED)
                .build();
    }

    /**
     * 빌더 스타일로 FriendRequest 생성을 시작합니다.
     *
     * @return FriendRequestBuilder 인스턴스
     */
    public static FriendRequestBuilder builder() {
        return new FriendRequestBuilder();
    }

    /**
     * FriendRequest 테스트 빌더.
     */
    public static class FriendRequestBuilder {
        private Long id = 1L;
        private Long requesterId = 1L;
        private Long receiverId = 2L;
        private FriendRequest.RequestStatus status = FriendRequest.RequestStatus.PENDING;

        /**
         * 요청 ID를 설정한다.
         *
         * @param id 요청 ID
         * @return 빌더
         */
        public FriendRequestBuilder id(Long id) {
            this.id = id;
            return this;
        }

        /**
         * 요청자 ID를 설정한다.
         *
         * @param requesterId 요청자 ID
         * @return 빌더
         */
        public FriendRequestBuilder requesterId(Long requesterId) {
            this.requesterId = requesterId;
            return this;
        }

        /**
         * 수신자 ID를 설정한다.
         *
         * @param receiverId 수신자 ID
         * @return 빌더
         */
        public FriendRequestBuilder receiverId(Long receiverId) {
            this.receiverId = receiverId;
            return this;
        }

        /**
         * 요청 상태를 설정한다.
         *
         * @param status 요청 상태
         * @return 빌더
         */
        public FriendRequestBuilder status(FriendRequest.RequestStatus status) {
            this.status = status;
            return this;
        }

        /**
         * FriendRequest 객체를 생성한다.
         *
         * @return 생성된 FriendRequest 엔티티
         */
        public FriendRequest build() {
            return FriendRequest.builder()
                    .id(id)
                    .requesterId(requesterId)
                    .receiverId(receiverId)
                    .status(status)
                    .build();
        }
    }
}
