package com.cotalk.adapter.outbound.persistence.mapper;

import com.cotalk.domain.entity.User;
import com.cotalk.adapter.outbound.persistence.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

/**
 * User 도메인과 UserJpaEntity 간 매핑.
 *
 * @author seunggu.lee
 */
@Component
public class UserMapper {

    public User toDomain(UserJpaEntity jpa) {
        if (jpa == null) {
            return null;
        }
        return User.builder()
                .id(jpa.getId())
                .email(jpa.getEmail())
                .passwordHash(jpa.getPasswordHash())
                .nickname(jpa.getNickname())
                .avatarUrl(jpa.getAvatarUrl())
                .oauthProvider(jpa.getOauthProvider())
                .oauthId(jpa.getOauthId())
                .status(jpa.getStatus())
                .role(jpa.getRole())
                .onlineStatus(jpa.getOnlineStatus())
                .lastActiveAt(jpa.getLastActiveAt())
                .statusMessage(jpa.getStatusMessage())
                .backgroundUrl(jpa.getBackgroundUrl())
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .build();
    }

    public UserJpaEntity toJpa(User domain) {
        if (domain == null) {
            return null;
        }
        UserJpaEntity jpa = UserJpaEntity.builder()
                .id(domain.getId())
                .email(domain.getEmail())
                .passwordHash(domain.getPasswordHash())
                .nickname(domain.getNickname())
                .avatarUrl(domain.getAvatarUrl())
                .oauthProvider(domain.getOauthProvider())
                .oauthId(domain.getOauthId())
                .status(domain.getStatus())
                .role(domain.getRole())
                .onlineStatus(domain.getOnlineStatus())
                .lastActiveAt(domain.getLastActiveAt())
                .statusMessage(domain.getStatusMessage())
                .backgroundUrl(domain.getBackgroundUrl())
                .build();
        if (domain.getCreatedAt() != null) {
            jpa.setCreatedAt(domain.getCreatedAt());
        }
        if (domain.getUpdatedAt() != null) {
            jpa.setUpdatedAt(domain.getUpdatedAt());
        }
        return jpa;
    }
}
