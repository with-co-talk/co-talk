package com.cotalk.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 비밀번호 재설정 토큰 엔티티.
 * 사용자의 비밀번호 재설정 요청에 대한 토큰 정보를 나타낸다.
 *
 * @author seunggu.lee
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PasswordResetToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String email;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /**
     * 비밀번호 재설정 토큰을 생성한다.
     *
     * @param userId 사용자 ID
     * @param email 이메일 주소
     * @param expirationMinutes 만료 시간 (분 단위)
     * @return 생성된 PasswordResetToken 인스턴스
     */
    public static PasswordResetToken create(Long userId, String email, int expirationMinutes) {
        return PasswordResetToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(userId)
                .email(email)
                .expiresAt(LocalDateTime.now().plusMinutes(expirationMinutes))
                .build();
    }

    /**
     * 토큰이 만료되었는지 확인한다.
     *
     * @return 만료되었으면 true, 그렇지 않으면 false
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 토큰이 사용되었는지 확인한다.
     *
     * @return 사용되었으면 true, 그렇지 않으면 false
     */
    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * 토큰이 유효한지 확인한다.
     * 만료되지 않고 사용되지 않은 경우 유효하다.
     *
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public boolean isValid() {
        return !isExpired() && !isUsed();
    }

    /**
     * 토큰을 사용됨으로 표시한다.
     */
    public void markAsUsed() {
        this.usedAt = LocalDateTime.now();
    }
}
