package com.cotalk.adapter.inbound.rest.dto.friend;

import java.time.LocalDateTime;

/**
 * 친구 요청 정보 DTO.
 *
 * @param id         친구 요청 ID
 * @param requester  요청자 정보
 * @param receiver   수신자 정보
 * @param status     요청 상태 (PENDING, ACCEPTED, REJECTED)
 * @param createdAt  생성 일시
 * @author seunggu.lee
 */
public record FriendRequestDto(
        Long id,
        FriendDto requester,
        FriendDto receiver,
        String status,
        LocalDateTime createdAt
) {
    /**
     * FriendRequestDto를 생성한다.
     *
     * @param id        친구 요청 ID
     * @param requester 요청자 DTO
     * @param receiver  수신자 DTO
     * @param status    요청 상태
     * @param createdAt 생성 일시
     * @return FriendRequestDto 인스턴스
     */
    public static FriendRequestDto of(Long id, FriendDto requester, FriendDto receiver, String status, LocalDateTime createdAt) {
        return new FriendRequestDto(id, requester, receiver, status, createdAt);
    }
}
