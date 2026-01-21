package com.cotalk.adapter.inbound.rest.dto.user;

import com.cotalk.domain.entity.User;
import com.cotalk.domain.entity.User.OnlineStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 사용자 정보 DTO.
 * User 엔티티의 정보를 클라이언트에 전달하기 위한 불변 객체이다.
 *
 * @param id           사용자 ID
 * @param email        이메일
 * @param nickname     닉네임
 * @param avatarUrl    아바타 URL
 * @param onlineStatus 온라인 상태
 * @param lastActiveAt 마지막 접속 시간
 * @author seunggu.lee
 */
public record UserDto(
        Long id,
        String email,
        String nickname,
        String avatarUrl,
        OnlineStatus onlineStatus,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime lastActiveAt
) {

    /**
     * User 엔티티로부터 UserDto를 생성한다.
     *
     * @param user User 엔티티
     * @return 변환된 UserDto
     * @throws NullPointerException user가 null인 경우
     */
    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getOnlineStatus(),
                user.getLastActiveAt()
        );
    }
}
