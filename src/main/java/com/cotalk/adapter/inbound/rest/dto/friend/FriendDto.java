package com.cotalk.adapter.inbound.rest.dto.friend;

import com.cotalk.domain.entity.User;

/**
 * 친구 정보 DTO.
 *
 * @param id       사용자 ID
 * @param nickname 닉네임
 * @param email    이메일
 * @author seunggu.lee
 */
public record FriendDto(Long id, String nickname, String email) {

    /**
     * User 엔티티로부터 FriendDto를 생성한다.
     *
     * @param user User 엔티티
     * @return FriendDto 인스턴스
     */
    public static FriendDto from(User user) {
        return new FriendDto(user.getId(), user.getNickname(), user.getEmail());
    }
}
