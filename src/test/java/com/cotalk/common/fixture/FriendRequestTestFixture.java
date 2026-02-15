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
}
