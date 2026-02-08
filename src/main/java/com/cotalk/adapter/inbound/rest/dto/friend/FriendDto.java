package com.cotalk.adapter.inbound.rest.dto.friend;

import com.cotalk.adapter.inbound.rest.dto.user.UserDto;
import com.cotalk.domain.entity.Friend;
import com.cotalk.domain.entity.User;

import java.time.LocalDateTime;

/**
 * 친구 정보 DTO.
 * Flutter 클라이언트의 FriendModel과 동일한 구조를 사용한다.
 *
 * @param id        친구 관계 ID
 * @param user      친구 사용자 정보 (중첩 구조)
 * @param createdAt 친구 관계 생성 일시
 * @param isHidden  숨김 여부
 * @author seunggu.lee
 */
public record FriendDto(
        Long id,
        UserDto user,
        LocalDateTime createdAt,
        boolean isHidden
) {

    /**
     * Friend 엔티티와 User 엔티티로부터 FriendDto를 생성한다.
     *
     * @param friend Friend 엔티티
     * @param user   친구의 User 엔티티
     * @return FriendDto 인스턴스
     */
    public static FriendDto from(Friend friend, User user) {
        return new FriendDto(
                friend.getId(),
                UserDto.from(user),
                friend.getCreatedAt(),
                false
        );
    }

    /**
     * User 엔티티로부터 FriendDto를 생성한다.
     * Friend 엔티티가 없는 경우 사용자 ID를 관계 ID로 사용한다.
     *
     * @param user User 엔티티
     * @return FriendDto 인스턴스
     */
    public static FriendDto from(User user) {
        return new FriendDto(
                user.getId(),
                UserDto.from(user),
                user.getCreatedAt(),
                false
        );
    }
}
