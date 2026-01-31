package com.cotalk.adapter.inbound.rest.dto.friend;

import com.cotalk.domain.entity.User;

import java.time.LocalDateTime;

/**
 * 친구 정보 DTO.
 *
 * @param id            사용자 ID
 * @param nickname      닉네임
 * @param email         이메일
 * @param avatarUrl     프로필 이미지 URL
 * @param onlineStatus  온라인 상태 (ONLINE, OFFLINE, AWAY)
 * @param lastActiveAt  마지막 활동 시간
 * @author seunggu.lee
 */
public record FriendDto(
        Long id,
        String nickname,
        String email,
        String avatarUrl,
        String onlineStatus,
        LocalDateTime lastActiveAt
) {

    /**
     * User 엔티티로부터 FriendDto를 생성한다.
     *
     * @param user User 엔티티
     * @return FriendDto 인스턴스
     */
    public static FriendDto from(User user) {
        return new FriendDto(
                user.getId(),
                user.getNickname(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getOnlineStatus().name(),
                user.getLastActiveAt()
        );
    }
}
