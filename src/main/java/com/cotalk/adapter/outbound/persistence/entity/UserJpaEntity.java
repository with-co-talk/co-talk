package com.cotalk.adapter.outbound.persistence.entity;

import com.cotalk.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자 JPA 엔티티.
 * persistence 계층 전용이며, 도메인 User와 매핑된다.
 *
 * @author seunggu.lee
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserJpaEntity extends BaseJpaEntity {

    @Id
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false)
    private String nickname;

    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider")
    private User.OAuthProvider oauthProvider;

    @Column(name = "oauth_id")
    private String oauthId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private User.UserStatus status = User.UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private User.Role role = User.Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "online_status", nullable = false)
    @Builder.Default
    private User.OnlineStatus onlineStatus = User.OnlineStatus.OFFLINE;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = true;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "status_message", length = 60)
    private String statusMessage;

    @Column(name = "background_url", length = 500)
    private String backgroundUrl;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
}
