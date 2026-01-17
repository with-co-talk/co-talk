package com.cotalk.adapter.inbound.rest.dto.block;

import com.cotalk.domain.entity.User;

/**
 * 차단된 사용자 정보 DTO.
 *
 * @param id        사용자 ID
 * @param nickname  닉네임
 * @param avatarUrl 아바타 URL
 * @author seunggu.lee
 */
public record BlockedUserDto(Long id, String nickname, String avatarUrl) {

    /**
     * User 엔티티로부터 BlockedUserDto를 생성한다.
     *
     * @param user User 엔티티
     * @return BlockedUserDto 인스턴스
     */
    public static BlockedUserDto from(User user) {
        return new BlockedUserDto(user.getId(), user.getNickname(), user.getAvatarUrl());
    }
}
