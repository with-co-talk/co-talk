package com.cotalk.adapter.inbound.rest.dto.friend;

import com.cotalk.adapter.inbound.rest.dto.user.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;

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
        UserDto requester,
        UserDto receiver,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
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
    public static FriendRequestDto of(Long id, UserDto requester, UserDto receiver, String status, LocalDateTime createdAt) {
        return new FriendRequestDto(id, requester, receiver, status, createdAt);
    }
}
