package com.cotalk.adapter.inbound.rest.dto.admin;

import com.cotalk.domain.entity.User;

import java.time.LocalDateTime;

/**
 * 관리자용 사용자 정보 DTO.
 *
 * @param id           사용자 ID
 * @param email        이메일
 * @param nickname     닉네임
 * @param avatarUrl    아바타 URL
 * @param status       계정 상태
 * @param onlineStatus 온라인 상태
 * @param createdAt    생성 일시
 * @param lastActiveAt 마지막 활동 일시
 * @author seunggu.lee
 */
public record AdminUserDto(
        Long id,
        String email,
        String nickname,
        String avatarUrl,
        String status,
        String onlineStatus,
        LocalDateTime createdAt,
        LocalDateTime lastActiveAt
) {
    /**
     * User 엔티티로부터 DTO를 생성한다.
     *
     * @param user User 엔티티
     * @return AdminUserDto 인스턴스
     */
    public static AdminUserDto from(User user) {
        return new AdminUserDto(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getStatus().name(),
                user.getOnlineStatus().name(),
                user.getCreatedAt(),
                user.getLastActiveAt()
        );
    }
}
